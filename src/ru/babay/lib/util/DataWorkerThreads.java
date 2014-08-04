package ru.babay.lib.util;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 19.12.12
 * Time: 17:19
 */
public class DataWorkerThreads {
    private static final int IMAGE_THREADS = 4;
    private static final int DATA_THREADS = 4;
    WorkerThreads mImageDownloadThreads;
    WorkerThreads mDataThreads;
    WorkerThreads mImageLoadThread;
    Handler delayHandler;
    private static DataWorkerThreads mInstance;

    public DataWorkerThreads() {
        mImageDownloadThreads = new WorkerThreads(IMAGE_THREADS);
        mDataThreads = new WorkerThreads(DATA_THREADS);
        HandlerThread thread = new HandlerThread("delay thread");
        thread.start();
        delayHandler = new Handler(thread.getLooper());
        mImageLoadThread = new WorkerThreads(8);
    }

    public static DataWorkerThreads getInstance() {
        if (mInstance == null)
            mInstance = new DataWorkerThreads();
        return mInstance;
    }

    public static void terminate() {
        if (mInstance != null) {
            mInstance.mImageDownloadThreads.terminate();
            mInstance.mDataThreads.terminate();
            mInstance.mImageLoadThread.terminate();
            mInstance.delayHandler.getLooper().quit();
            mInstance = null;
        }
    }

    public void postToImageThreads(Runnable runnable) {
        mImageDownloadThreads.run(runnable);
    }

    public void postToDataThreads(Runnable runnable) {
        mDataThreads.run(runnable);
    }

    public static void postToImageDownloadThreads(Runnable runnable) {
        getInstance().mImageDownloadThreads.run(runnable);
    }

    public static void postToImageDownloadThreads(final Runnable runnable, int delay) {
        if (delay == 0)
            getInstance().mImageDownloadThreads.run(runnable);
        else
        getInstance().delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getInstance().mImageDownloadThreads.run(runnable);
            }
        }, delay);
    }

    public static void postToImageLoadThread(Runnable runnable) {
        getInstance().mImageLoadThread.run(runnable);
    }

    public static void postToImageLoadThread(final Runnable runnable, int delay) {
        if (delay == 0)
            getInstance().mImageLoadThread.run(runnable);
        else
            getInstance().delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getInstance().mImageLoadThread.run(runnable);
                }
            }, delay);
    }

    public static void postToDataThreadsS(Runnable runnable) {
        getInstance().mDataThreads.run(runnable);
    }

    public static int countDataThreadsActiveTask() {
        return getInstance().mDataThreads.getTotalQueueLength();
    }

    public static int countImageThreadsActiveTask() {
        return getInstance().mDataThreads.getTotalQueueLength();
    }

    public static void setPoolListener(boolean isDataPool, WorkerThreads.PoolListener listener) {
        if (isDataPool)
            getInstance().mDataThreads.setPoolListener(listener);
        else
            getInstance().mImageDownloadThreads.setPoolListener(listener);
    }


    public void setDataWorkerThreadsSize(int size) {
        if (size == 0)
            size = DATA_THREADS;
        mDataThreads.setMaxThreads(size);
    }

    public void postDelayed(Runnable r, long delay) {
        delayHandler.postDelayed(r, delay);
    }
}
