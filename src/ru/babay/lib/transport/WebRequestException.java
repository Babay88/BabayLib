package ru.babay.lib.transport;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 21.02.13
 * Time: 15:07
 */
public class WebRequestException extends Exception {
    int errorCode;
    String requestData;

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
}

