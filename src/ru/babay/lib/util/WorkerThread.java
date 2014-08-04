package ru.babay.lib.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Created by IntelliJ IDEA.
 * User: babay
 * Date: 14.04.12
 * Time: 13:28
 */
public class WorkerThread {
    private static WorkerThread instance;
    HandlerThread thread;
    Looper looper;
    Handler handler;

    public WorkerThread() {
        thread = new HandlerThread("worker thread");
        thread.start();
        looper = thread.getLooper();
        handler = new Handler(looper);
    }

    public static WorkerThread getInstance(){
        if (instance == null)
            instance = new WorkerThread();
        return instance;
    }

    public void post(Runnable r){
        handler.post(r);
    }

    public void postDelayed(Runnable r, long delayMillis){
        handler.postDelayed(r, delayMillis);
    }

    public void dispose(){
        thread.quit();
        instance = null;
    }
}
