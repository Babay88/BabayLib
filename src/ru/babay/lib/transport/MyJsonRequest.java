package ru.babay.lib.transport;

import android.text.TextUtils;
import android.util.Log;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.babay.lib.BugHandler;
import ru.babay.lib.Settings;
import ru.babay.lib.util.Util;

import java.io.*;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public abstract class MyJsonRequest implements Runnable {
    public enum RequestType {Get, Post, Put, Delete}

    public enum ResultType {JsonArray, JsonObject, String}

    private static final int SOCKET_TIMEOUT = 15000;
    private static final boolean ENABLE_GZIP = true;
    boolean debug;
    boolean logDebugData;
    boolean reportDebugData;
    boolean alreadyRun = false;
    boolean done = false;
    boolean aborted = false;

    JSONObject jsonParam;
    //Map params;
    List<NameValuePair> params;
    List<String> methodParams;
    String methodName;

    HttpRequestBase mHttpRequest;
    RequestType mRequestType;
    ResultType mResultType;
    String mRequestBase;
    //String requestDetails;

    long startTime;
    long endTime;
    boolean isJsonRequest;
    boolean dontSendContentType = false;
    StringBuilder debugData;
    File file;
    InputStream attachInputStream;
    String multipartParamName = "Filedata";

    File logToFile;

    protected MyJsonRequest(String requestBaseUri, RequestType requestType, ResultType resultType) {
        mRequestBase = requestBaseUri;
        //mRequestType = requestType;
        mResultType = resultType;
        setRequestType(requestType);
    }

    protected abstract void onSuccess(Object object) throws JSONException;

    protected abstract void onError(Exception error);

    protected abstract void onAbort();

    protected abstract boolean isResponceOk(Object object) throws JSONException;

    protected abstract WebRequestException parseError(Object object);

    public void addParam(String name, String value) {
        if (isJsonRequest) {
            if (jsonParam == null)
                jsonParam = new JSONObject();
            try {
                jsonParam.put(name, value);
            } catch (JSONException e) {
            }
        } else {
            if (this.params == null)
                this.params = new ArrayList<NameValuePair>();

            params.add(new BasicNameValuePair(name, value));
        }
    }

    public void addParam(String name, int value) {
        if (isJsonRequest) {
            if (jsonParam == null)
                jsonParam = new JSONObject();
            try {
                jsonParam.put(name, value);
            } catch (JSONException e) {
            }
        } else {
            if (this.params == null)
                this.params = new ArrayList<NameValuePair>();

            params.add(new BasicNameValuePair(name, Integer.toString(value)));
        }
    }

    public void addParam(String name, JSONObject obj) {
        if (jsonParam == null)
            jsonParam = new JSONObject();
        try {
            jsonParam.put(name, obj);
        } catch (JSONException e) {
        }
    }

    public void addMethodParam(String value) {
        if (methodParams == null)
            methodParams = new ArrayList<String>();
        methodParams.add(value);
    }

    public void addMethodParam(int value) {
        addMethodParam(Integer.toString(value));
    }

    public void setParams(JSONObject jsonParam) {
        this.jsonParam = jsonParam;
    }

    @Override
    public void run() {
        try {
            Object result = runSyncPrivate();
            if (!aborted)
                onSuccess(result);
        } catch (Exception e){
            onError(e);
        }
    }

    public Object runSyncPrivate() throws Exception {
        synchronized (this) {
            if (alreadyRun)
                throw new IllegalStateException("can run request only once");
            alreadyRun = true;
        }

        debug = reportDebugData || logDebugData || logToFile != null;

        if (debug)
            debugData = new StringBuilder();

        if (methodName != null && mRequestBase.endsWith("/") && methodName.startsWith("/"))
            methodName = methodName.substring(1);

        String uri = methodName == null ? mRequestBase : mRequestBase + methodName;
        if (methodParams != null)
            uri += methodParamsToString();

        long requestStartTime = System.currentTimeMillis();
        String responce = null;
        try {
            switch (mRequestType) {
                case Post:
                case Put:
                    responce = sendEnclosingRequest(uri);
                    break;
                case Delete:
                case Get:
                    responce = getMessage(uri);
                    break;
            }

            if (debugData != null)
                debugData.append(String.format("responce time: %d ms, pure: %d ms\n", System.currentTimeMillis() - requestStartTime, endTime - startTime));

            if (logDebugData) {
                BugHandler.logD(debugData.toString());
            }


            Object responceObj = parseResult(responce);

            if (aborted) {
                onAbort();
                return null;
            }

            if (isResponceOk(responceObj)) {
                if (logToFile != null)
                    logToFile(debugData.toString());
                return responceObj;
            } else {
                WebRequestException exception = parseError(responceObj);
                if (debugData != null)
                    exception.setRequestData(debugData.toString());

                throw exception;
            }


        } catch (Exception e) {
            if (logDebugData) {
                BugHandler.logD(String.format("responce time: %d ms\n", System.currentTimeMillis() - requestStartTime));
                BugHandler.logE(String.format("%s, request: %s %s\n", e.getMessage(), mRequestType.toString(), debugData.toString()), e);
            }

            if (e instanceof SocketException)
                e = new WebRequestException("Связь с сервером внезапно прервалась", e, 0);
            else if (!(e instanceof WebRequestException))
                e = new WebRequestException(e);
            if (debugData != null)
                ((WebRequestException)e).setRequestData(debugData.toString());

            if (logToFile != null) {
                StringBuilder builder = new StringBuilder();
                builder.append(String.format("responce time: %d ms\n", System.currentTimeMillis() - requestStartTime));
                builder.append(String.format("%s, request: %s %s\n", e.getMessage(), mRequestType.toString(), debugData.toString()));
                builder.append(Log.getStackTraceString(e));
                builder.append(TextUtils.join("\n", e.getStackTrace()));

                logToFile(builder.toString());
            }
            if (aborted) {
                onAbort();
                return null;
            }
            else
                throw e;
        } finally {
        }
    }

    void logToFile(String data) {

        try {
            FileOutputStream stream = new FileOutputStream(logToFile, true);
            stream.write("\n\n".getBytes());
            stream.write(data.getBytes());
            stream.write("\n\n".getBytes());
            stream.flush();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    String getMessage(String path) throws Exception {
        long requestStartTime = System.currentTimeMillis();
        HttpRequestBase request = createHttpRequest(path, paramsToString(params));

        mHttpRequest = request;
        HttpClient client = getClient(request.getParams(), request.getURI());

        request.setHeader("Accept", "application/json");

        if (debugData != null) {
            String debugString = String.format("%s request: %s\n request headers: %s\n", mRequestType.toString(), mHttpRequest.getURI(), getHeadersString(request));
            debugData.append(debugString);
            BugHandler.logD(debugData.toString());
        }

        ResponseHandler responseHandler = new BasicResponseHandler();
        startTime = System.currentTimeMillis();
        String responce = (String) client.execute(request, responseHandler);
        endTime = System.currentTimeMillis();
        if (debugData != null) {
            debugData.append(String.format("\nresponce time: %d ms, pure: %d ms\n", System.currentTimeMillis() - requestStartTime, endTime - startTime));
            debugData.append(String.format("%s responce: %s\n%s\n", mRequestType.toString(), request.getURI(), responce));
        }

        done = true;
        if (aborted)
            return null;

        return responce;
    }

    String getHeadersString(HttpRequestBase request){
        Header[] headers = request.getAllHeaders();
        StringBuilder builder = new StringBuilder();
        for (int i=0; i< headers.length; i++){
            builder.append(headers[i].getName());
            builder.append(": ");
            builder.append(headers[i].getValue());
            builder.append("\n");
        }
        return builder.toString();
    }

    Object parseResult(String responce) throws JSONException {
        switch (mResultType) {
            case JsonObject:
                return new JSONObject(responce);
            case JsonArray:
                try {
                    return new JSONArray(responce);
                } catch (Exception e) {
                    return new JSONObject(responce);
                }
            case String:
                return responce;
        }
        return responce;
    }

    public void abort() {
        if (!alreadyRun || done || mHttpRequest == null)
            return;

        aborted = true;
        mHttpRequest.abort();
    }

    void enableLoging() {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
    }

    public String sendEnclosingRequest(String path) throws Exception {
        long requestStartTime = System.currentTimeMillis();
        if (debug)
            enableLoging();

        HttpEntityEnclosingRequestBase request = (HttpEntityEnclosingRequestBase) createHttpRequest(path, null);
        mHttpRequest = request;

        if (isJsonRequest) {
            if (jsonParam == null)
                jsonParam = paramsToJson(params);
            if (jsonParam != null) {
                StringEntity se = new StringEntity(jsonParam.toString(), "UTF-8");
                se.setContentType("application/json;charset=UTF-8");//text/plain;charset=UTF-8  //application/json;charset=UTF-8
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
                request.setEntity(se);
            }
            if (!dontSendContentType)
                request.setHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8");
            else
                request.setHeader(HTTP.CONTENT_TYPE, "text/plain;charset=UTF-8");

            if (debugData != null && jsonParam != null) {
                debugData.append(String.format("%s request: %s\nheaders: %s\nparams: %s\n", mRequestType.toString(), path, getHeadersString(request), jsonParam.toString()));
                }
        } else if (file != null || attachInputStream != null) {

            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            if (params != null)
                for (NameValuePair param : params)
                    reqEntity.addPart(param.getName(), new StringBody(param.getValue()));

            byte[] bytes = file != null ? Util.readFullyAsByteArray(file) : Util.readFullyAsByteArray(attachInputStream);
            ByteArrayBody bab = new ByteArrayBody(bytes, file.getName());
            reqEntity.addPart(multipartParamName, bab);

            if (debugData != null) {
                debugData.append(String.format("%s send file request: %s\n%s\n", mRequestType.toString(), path, file.getPath()));
                debugData.append(String.format("requestHeaders: %s\n", getHeadersString(request)));
                debugData.append(String.format("attach file as: %s", multipartParamName));
                }

            request.setEntity(reqEntity);
            //request.addHeader("Accept-Encoding", "gzip");

        } else if (params != null && params.size() > 0) {
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            request.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");

            if (debugData != null) {
                String paramData = Util.readFully(request.getEntity().getContent());
                debugData.append(String.format("request: %s %s", mRequestType.toString(), path));
                debugData.append(String.format("requestHeaders: %s\n", getHeadersString(request)));
                debugData.append(String.format("request body %s\n", paramData));
                }
            }

        HttpClient client = getClient(request.getParams(), request.getURI());

        /*request.setHeader("Accept", "application/json");*/

        if (debugData != null) {
            for (Header h : request.getAllHeaders())
                debugData.append(String.format("%s: %s\n", h.getName(), h.getValue()));
            BugHandler.logD(debugData.toString());
        }

        ResponseHandler responseHandler = new BasicResponseHandler();
        startTime = System.currentTimeMillis();
        String responce = (String) client.execute(request, responseHandler);
        endTime = System.currentTimeMillis();
        if (debugData != null) {
            debugData.append(String.format("\nresponce time: %d ms, pure: %d ms\n", System.currentTimeMillis() - requestStartTime, endTime - startTime));
            debugData.append(String.format("%s responce: %s\n%s\n", mRequestType.toString(), path, responce));
            }

        done = true;
        if (aborted)
            return null;

        return responce;
    }

    protected HttpRequestBase createHttpRequest(String path, String params) throws URISyntaxException {
        if (params != null){
            if (path.contains("?"))
                path = path + "&";
            else
                path = path + "?";
            path += params;
        }
        HttpRequestBase request = null;
        switch (mRequestType) {
            case Post:
                request = new HttpPost(new URI(path));
                break;
            case Put:
                request = new HttpPut(new URI(path));
                break;
            case Get:
                request = new HttpGet(new URI(path));
                break;
            case Delete:
                request = new HttpDelete(new URI(path));
                break;
        }

        return request;
    }

    HttpClient getClient(HttpParams params, URI uri) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        KeyStore trusted = KeyStore.getInstance("BKS");
        trusted.load(null, null);

        int port = uri.getPort();
        String scheme = uri.getScheme();
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        if (scheme.equals("http"))
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), port != -1 ? port : 80));
        else if (scheme.equals("https"))
            schemeRegistry.register(new Scheme("https", TrustAllSSLSocketFactory.getInstance(), port != -1 ? port : 443));

        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        SingleClientConnManager cm = new SingleClientConnManager(params, schemeRegistry);
        DefaultHttpClient client = new DefaultHttpClient(cm, params);
        if (ENABLE_GZIP)
            attachGzipCompression(client);

        return client;
    }

    void attachGzipCompression(DefaultHttpClient client){
        client.addRequestInterceptor(new HttpRequestInterceptor() {

            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }

        });

        client.addResponseInterceptor(new HttpResponseInterceptor() {

            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Header ceheader = entity.getContentEncoding();
                    if (ceheader != null) {
                        HeaderElement[] codecs = ceheader.getElements();
                        for (int i = 0; i < codecs.length; i++) {
                            if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                response.setEntity(
                                        new GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            }

        });
    }

    JSONObject paramsToJson(List<NameValuePair> params) throws JSONException {
        JSONObject holder = new JSONObject();

        for (NameValuePair pair : params) {
            holder.put(pair.getName(), pair.getValue());
        }

        /*for (Object o : params.entrySet()) {
            Map.Entry pairs = (Map.Entry) o;
            String key = (String) pairs.getKey();
            Object value = pairs.getValue();
            if (value instanceof Map)
                holder.put(key, paramsToJson((Map) value));
            else
                holder.put(key, value);
        }*/
        return holder;
    }

    String paramsToString(List<NameValuePair> params) {
        StringBuilder builder = new StringBuilder();

        for (NameValuePair pair : params) {
            try {
                builder.append(URLEncoder.encode(pair.getName(), "utf-8"));
                if (pair.getValue() != null) {
                    builder.append("=");
                    builder.append(URLEncoder.encode(pair.getValue(), "utf-8"));
                }
            } catch (UnsupportedEncodingException e) {
            }
            builder.append("&");
        }

        return builder.toString();
    }

    String methodParamsToString() {
        if (methodParams == null)
            return "";

        StringBuilder builder = new StringBuilder();
        for (String param : methodParams) {
            try {
                builder.append(URLEncoder.encode(param, "utf-8"));
                builder.append("/");
            } catch (UnsupportedEncodingException e) {
            }
        }
        return builder.toString();
    }

    public void setMethod(String method) {
        this.methodName = method;
    }

    public void setResultType(ResultType type) {
        this.mResultType = type;
    }

    public void setRequestBase(String requestBase) {
        this.mRequestBase = requestBase;
    }

    public void setJsonRequest(boolean jsonRequest) {
        isJsonRequest = jsonRequest;
        if (params != null && params.size() > 0)
            try {
                jsonParam = paramsToJson(params);
            } catch (JSONException e) {
            }
    }

    public void setDontSendContentType(boolean dontSendContentType) {
        this.dontSendContentType = dontSendContentType;
    }

    public void setReportDebugData(boolean reportDebugData) {
        this.reportDebugData = reportDebugData;
    }

    public void setLogDebugData(boolean logDebugData) {
        this.logDebugData = logDebugData;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setAttachInputStream(InputStream attachInputStream) {
        this.attachInputStream = attachInputStream;
    }

    public void setLogToFile(File logToFile) {
        this.logToFile = logToFile;
    }

    public void setRequestType(RequestType requestType) {
        this.mRequestType = requestType;
        isJsonRequest = requestType == RequestType.Post || requestType == RequestType.Put;
    }

    public void setMultipartParamName(String multipartFileParamName) {
        this.multipartParamName = multipartFileParamName;
    }
}