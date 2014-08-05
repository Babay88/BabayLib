package ru.babay.lib;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 22.04.2014
 * Time: 21:10
 */
public abstract class BugHandler {

    static BugHandler handlerInstance;

    public enum LogLevel {Debug, Warning, Error}

    public static void sendException(Exception e) {
        if (handlerInstance != null)
            handlerInstance.sendExceptionImp(e);
    }

    public static void sendExceptionMap(HashMap<String, String> debugMap, Exception error) {
        if (handlerInstance != null)
            handlerInstance.sendExceptionMapImp(debugMap, error);
    }

    public static void sendExceptionMessage(String exception, String message, Exception error) {
        if (handlerInstance != null)
            handlerInstance.sendExceptionMessageImp(exception, message, error);
    }

    public static void logE(Throwable e) {
        if (handlerInstance != null)
            handlerInstance.logInt(LogLevel.Error, null, e);
    }

    public static void logE(String message, Throwable e) {
        if (handlerInstance != null)
            handlerInstance.logInt(LogLevel.Error, message, e);
    }

    public static void logW(Throwable e) {
        if (handlerInstance != null)
            handlerInstance.logInt(LogLevel.Warning, null, e);
    }

    public static void logD(Throwable e) {
        if (handlerInstance != null)
            handlerInstance.logInt(LogLevel.Debug, null, e);
    }

    public static void logD(String message) {
        if (handlerInstance != null)
            handlerInstance.logInt(LogLevel.Debug, message, null);
    }

    public static void setHandlerInstance(BugHandler handlerInstance) {
        BugHandler.handlerInstance = handlerInstance;
    }

    protected abstract void sendExceptionImp(Exception e);

    protected abstract void sendExceptionMapImp(HashMap<String, String> debugMap, Exception error);

    protected abstract void sendExceptionMessageImp(String exception, String message, Exception error);

    protected abstract void logInt(LogLevel level, String message, Throwable e);
}