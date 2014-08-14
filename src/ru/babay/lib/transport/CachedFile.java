package ru.babay.lib.transport;

import android.content.Context;
import android.util.Log;
import ru.babay.lib.BugHandler;
import ru.babay.lib.Settings;
import ru.babay.lib.util.FileHelper;
import ru.babay.lib.util.Util;
import ru.babay.lib.util.WorkerThread;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

//Version 1.1

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 04.08.12
 * Time: 14:16
 * To change this template use File | Settings | File Templates.
 */
public class CachedFile implements Runnable {
    public enum Status {None, Started, Stopped, Completed, Aborted}

    Context context;
    String path;
    String urlStr;
    CachedFileListener listener;
    private static final long REFRESH_INTERVAL = 24 * 60 * 60 * 1000;
    boolean forceUpdate;
    boolean dontRefresh;
    TTL ttl = TTL.Day;
    boolean useExternalStorage;
    InputStream responseStream;
    FileOutputStream outFile;
    Status status = Status.None;

    public CachedFile(Context context, String path, String url, TTL ttl, CachedFileListener listener) {
        this.context = context;
        this.path = path;
        this.urlStr = url;
        this.listener = listener;
        this.ttl = ttl;
    }

    public void setUseExternalStorage(boolean useExternalStorage) {
        this.useExternalStorage = useExternalStorage;
    }

    public CachedFile(Context context) {
        this.context = context;
    }

    public void setData(String path, String url) {
        this.path = path;
        this.urlStr = url;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public void setDontRefresh(boolean dontRefresh) {
        this.dontRefresh = dontRefresh;
    }

    public void setListener(CachedFileListener listener) {
        this.listener = listener;
    }

    public static File getFile(Context context, String path, boolean isExternal) {
        File file = Util.makePathAndGetFile(context, path, isExternal);
        return file.exists() ? file : null;
    }

    @Override
    public void run() {
        if (status != Status.None) {
            return;
        }
        status = Status.Started;

        File file = Util.makePathAndGetFile(context, path, useExternalStorage);

        boolean alreadyHaveFile = file.exists() && !forceUpdate;
        if (alreadyHaveFile) {
            listener.onGotFile(file, this);
            if (!shouldUpdate(file)) {
                status = Status.Completed;
                return;
            }
        }

        BugHandler.logD("loading file: " + urlStr);

        long timeStart = System.currentTimeMillis();

        try {
            URL url = new URL(urlStr);

            responseStream = url.openConnection().getInputStream();

            outFile = new FileOutputStream(file);
            FileHelper.copyFile(responseStream, outFile);
            outFile.close();

            BugHandler.logD(String.format("loaded file: %s, %d ms", url, System.currentTimeMillis() - timeStart));

            if (status != Status.Aborted) {
                status = Status.Completed;
                file.setLastModified(System.currentTimeMillis());
                listener.onGotFile(file, this);
            }
            //if (!alreadyHaveFile)
            //    listener.onGotFile(file);

        } catch (MalformedURLException | SecurityException | NullPointerException e) {
            status = Status.Stopped;
            listener.onError(e, this);
        } catch (IOException e) {
            status = Status.Stopped;
            if (status == Status.Aborted && file != null && file.exists())
                file.delete();
            try {
                outFile.close();
            } catch (Throwable t) {
            }
            listener.onError(e, this);
        }
    }

    boolean shouldUpdate(File file) {
        return forceUpdate || !file.exists() || (!dontRefresh && ((System.currentTimeMillis() - file.lastModified()) >= ttl.getDurationMs()));
    }

    public void abort() {
        if (status == Status.Stopped || status == Status.Completed || status == Status.Aborted)
            return;
        status = Status.Aborted;
        WorkerThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (responseStream != null)
                    try {
                        responseStream.close();
                    } catch (IOException e) {
                    }
            }
        });

    }

    public boolean isAborted() {
        return status == Status.Aborted;
    }

    public static interface CachedFileListener {
        public void onGotFile(File file, CachedFile cachedFile);

        public void onUpdateFile(File file, CachedFile cachedFile);

        public void onError(Throwable t, CachedFile cachedFile);
    }
}
