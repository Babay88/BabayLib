package ru.babay.lib.transport;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 21.02.13
 * Time: 15:07
 */
public class WebRequestException extends Exception {
    protected int errorCode;
    protected String requestData;
    HashMap<String, String> debugDataMap;

    public WebRequestException(String detailMessage, int errorCode) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public WebRequestException(String detailMessage, Throwable throwable, int errorCode) {
        super(detailMessage, throwable);
        this.errorCode = errorCode;
    }

    public WebRequestException(String detailMessage) {
        super(detailMessage);
    }

    public WebRequestException(Throwable throwable) {
        super(throwable);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public HashMap<String, String> getDebugDataMap() {
        return debugDataMap;
    }

    public String getDebugData(){
        if (debugDataMap == null)
            return "";
        StringBuilder builder = new StringBuilder();
        for (String key: debugDataMap.keySet()){
            builder.append(key);
            builder.append(": ");
            builder.append(debugDataMap.get(key));
            builder.append("\n");
        }
        return builder.toString();
    }

    public void setDebugDataMap(HashMap<String, String> debugDataMap) {
        this.debugDataMap = debugDataMap;
    }
}

