package ru.babay.lib.transport;

import org.json.JSONException;
import org.json.JSONObject;
import ru.babay.lib.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 12.12.12
 * Time: 13:14
 */
public abstract class BasicRequest<T> extends MyJsonRequest {
    protected ItemListener<T> mListener;

    protected BasicRequest(String requestBaseUri, RequestType requestType, ResultType resultType, ItemListener<T> mListener) {
        super(requestBaseUri, requestType, resultType);
        this.mListener = mListener;
    }

    public interface ItemListener<T> {
        void onLoaded(T item);

        void onError(Exception error);

        void onLoadedStored(T item);
    }

    @Override
    protected void onAbort() {
    }

    @Override
    protected void onError(Exception error) {
        mListener.onError(error);
    }



    @Override
    protected void onSuccess(Object object) throws JSONException {
        T data = parse(object);
        if (mListener != null)
            mListener.onLoaded(data);
    }

    @Override
    protected WebRequestException parseError(Object object) {
        if (object instanceof JSONObject) {
            JSONObject responce = (JSONObject) object;
            try {
                String msg = responce.getString("sMsg");
                int errorCode = 0;
                if (responce.has("response")) {
                    JSONObject responseData = responce.getJSONObject("response");
                    if (responseData.has("error_code"))
                        errorCode = responseData.getInt("error_code");
                }
                if (msg != null && msg.length() > 0 || errorCode != 0)
                    return new WebRequestException(msg, errorCode);
            } catch (JSONException e) {
            }

            try {
                JSONObject data = responce.getJSONObject("response");
                return new WebRequestException(data.getString("message"));
            } catch (JSONException e) {
            }

        }

        return new WebRequestException(object.toString());
    }

    protected abstract T parse(Object source) throws JSONException;
}
