package ru.babay.lib.util;

import android.os.Handler;
import android.os.HandlerThread;

import java.lang.ref.WeakReference;

/**
 * Created by IntelliJ IDEA.
 * User: babay
 * Date: 18.06.12
 * Time: 13:18
 */
public class WorkerThreads {
    private static final int MAX_THREADS = 3;
    private static WorkerThreads instance;
    protected int maxThreads;

    private final Array<HandlerWrapper> activeHandlers;
    //private Pool<HandlerWrapper> handlerPool;
    private WeakReference<PoolListener> mPoolListenerRef;

    public WorkerThreads() {
        this(MAX_THREADS);
    }

    public WorkerThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        activeHandlers = new Array<HandlerWrapper>(maxThreads);
    }

    public static WorkerThreads getInstance() {
        if (instance == null)
            instance = new WorkerThreads();

        return instance;
    }

    public void terminate() {
        synchronized (activeHandlers) {
            try {
                while (activeHandlers.size > 0) {
                    HandlerWrapper h = activeHandlers.get(0);
                    h.thread.quit();
                    activeHandlers.removeIndex(0);
                }
            } catch (Exception e) {
            }
        }
    }

    static HandlerWrapper getFreeHandler() {
        if (instance == null)
            instance = new WorkerThreads();

        return instance.getHandler();
    }

    HandlerWrapper getHandler() {
        synchronized (activeHandlers) {
            if (activeHandlers.size < MAX_THREADS) {
                HandlerThread thread = new HandlerThread("");
                thread.start();
                HandlerWrapper wrapper = new HandlerWrapper(new Handler(thread.getLooper()), thread);
                activeHandlers.add(wrapper);
                return wrapper;
            } else {
                HandlerWrapper cur, min = activeHandlers.get(0);
                int minQueue = min.queueLength;

                for (int i = 1; i < MAX_THREADS; i++) {
                    cur = activeHandlers.get(i);
                    if (cur.queueLength < minQueue)
                        min = cur;
                }
                return min;
            }
        }
    }

    void killHandler(HandlerWrapper handler) {
        synchronized (activeHandlers) {
            activeHandlers.removeValue(handler, true);
        }
        if (activeHandlers.size == 0 && mPoolListenerRef != null && mPoolListenerRef.get() != null)
            mPoolListenerRef.get().onPoolEmpty();
    }

    public int getTotalQueueLength() {
        int size = 0;
        for (HandlerWrapper handler : activeHandlers)
            size += handler.queueLength;
        return size;
    }

    public void run(Runnable runnable) {
        getHandler().run(runnable);
    }

    public void run(final Runnable[] runnables) {
        getInstance().run(runnables);
    }

    public static void runS(Runnable runnable) {
        getFreeHandler().run(runnable);
    }

    public static void runS(final Runnable[] runnables) {
        for (Runnable runnable : runnables)
            getFreeHandler().run(runnable);
    }


    class HandlerWrapper {
        Handler handler;
        HandlerThread thread;
        volatile public int queueLength = 0;

        HandlerWrapper(Handler handler, HandlerThread thread) {
            this.handler = handler;
            this.thread = thread;
        }

        public void run(final Runnable r) {
            queueLength++;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    r.run();
                    queueLength--;
                    if (queueLength == 0) {
                        killHandler(HandlerWrapper.this);
                        thread.quit();
                    }
                }
            });
        }
    }

    public void setPoolListener(PoolListener poolListener) {
        if (poolListener == null)
            mPoolListenerRef = null;
        else
            mPoolListenerRef = new WeakReference<PoolListener>(poolListener);
    }

    public interface PoolListener {
        void onPoolEmpty();
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }
}
