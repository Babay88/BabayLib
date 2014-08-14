package ru.babay.lib;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 13.12.12
 * Time: 17:42
 */
public class NotImplementedException extends RuntimeException {
    public NotImplementedException() {
    }

    public NotImplementedException(String detailMessage) {
        super(detailMessage);
    }

    public NotImplementedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NotImplementedException(Throwable throwable) {
        super(throwable);
    }
}
