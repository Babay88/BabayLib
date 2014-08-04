package ru.babay.lib.util;

import android.content.Context;
import ru.babay.lib.transport.ImageCache;
import ru.babay.lib.transport.TTL;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 13.12.12
 * Time: 21:40
 */
public class CacheCleanup implements Runnable {
    protected Context mContext;
    protected boolean isExternal;

    public CacheCleanup(Context mContext, boolean isExternal) {
        this.mContext = mContext;
        this.isExternal = isExternal;
    }

    @Override
    public void run() {
        cleanupImages(TTL.Day);
        cleanupImages(TTL.TwoDays);
        cleanupImages(TTL.Week);
    }

    void cleanupImages(TTL ttl) {
        String path = ImageCache.getFolderFor(ttl);
        File dir = Util.makePathAndGetFile(mContext, path, isExternal);
        File[] files = dir.listFiles();
        long maxAge = ttl.getDurationMs();
        long now = System.currentTimeMillis();
        if (files != null) {
            for (File file : files) {
                if (now - file.lastModified() > maxAge)
                    file.delete();
            }
        }
    }
}
