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

    public static void setHandlerInstance(BugHandler handlerInstance) {
        BugHandler.handlerInstance = handlerInstance;
    }

    protected abstract void sendExceptionImp(Exception e);

    protected abstract void sendExceptionMapImp(HashMap<String, String> debugMap, Exception error);

    protected abstract void sendExceptionMessageImp(String exception, String message, Exception error);
}