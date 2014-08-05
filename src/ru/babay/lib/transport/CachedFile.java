package ru.babay.lib.transport;

import android.content.Context;
import android.util.Log;
import ru.babay.lib.BugHandler;
import ru.babay.lib.Settings;
import ru.babay.lib.util.FileHelper;
import ru.babay.lib.util.Util;
import ru.babay.lib.util.WorkerThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    Context context;
    String path;
    String urlStr;
    CachedFileListener listener;
    private static final long REFRESH_INTERVAL = 24 * 60 * 60 * 1000;
    boolean forceUpdate;
    boolean dontRefresh;
    TTL ttl = TTL.Day;
    boolean useExternalStorage;
    boolean aborted;
    InputStream responseStream;
    FileOutputStream outFile;


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

    /*public static File getWebViewFile(Context context, String url) {
        String hashCode = String.format("%08x", url.hashCode());
        File file = new File(new File(context.getCacheDir(), "webviewCache"), hashCode);
        return file.exists() ? file : null;
    }*/



    @Override
    public void run() {
        if (aborted)
            return;
        File /*file = getWebViewFile(context, urlStr);
        if (file == null)*/
            file = Util.makePathAndGetFile(context, path, useExternalStorage);

        boolean alreadyHaveFile = file.exists() && !forceUpdate;
        if (alreadyHaveFile) {
            listener.onGotFile(file, this);
            if (!shouldUpdate(file))
                return;
        }

        BugHandler.logD("loading file: " + urlStr);

        long timeStart = System.currentTimeMillis();

        try {
            URL url = new URL(urlStr);

            responseStream = url.openConnection().getInputStream();

            //ByteArrayOutputStream downloadStream = new ByteArrayOutputStream();

            outFile = new FileOutputStream(file);
            FileHelper.copyFile(responseStream, outFile);
            outFile.close();
            //byte [] newFileBytes = downloadStream.toByteArray();
            //downloadStream.reset();

            BugHandler.logD(String.format("loaded file: %s, %d ms", url, System.currentTimeMillis() - timeStart));

            //byte oldFileBytes[] = null;

            /*if (file.exists()){
                FileInputStream oldFileStream = new FileInputStream(file);
                oldFileBytes = new byte[oldFileStream.available()];
                oldFileStream.read(oldFileBytes, 0, oldFileBytes.length);
                oldFileStream.close();
            }*/

            /*if (!Arrays.equals(newFileBytes, oldFileBytes)){
                FileOutputStream outFile = new FileOutputStream(file);
                outFile.write(newFileBytes);
                outFile.flush();
                outFile.close();
                if (alreadyHaveFile)
                    listener.onUpdateFile(file);
                else
                    listener.onGotFile(file);
            } else*/
            listener.onGotFile(file, this);
            file.setLastModified(System.currentTimeMillis());
            //if (!alreadyHaveFile)
            //    listener.onGotFile(file);

        } catch (MalformedURLException e) {
            listener.onError(e, this);
        } catch (IOException e) {
            if (aborted && file != null && file.exists())
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
        aborted = true;
        if (responseStream != null)
            WorkerThread.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        responseStream.close();
                    } catch (Throwable e) {
                    }
                }
            });
    }

    public boolean isAborted() {
        return aborted;
    }

    public static interface CachedFileListener {
        public void onGotFile(File file, CachedFile cachedFile);

        public void onUpdateFile(File file, CachedFile cachedFile);

        public void onError(Throwable t, CachedFile cachedFile);
    }
}
