package ru.babay.lib.transport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.util.LruCache;
import ru.babay.lib.BugHandler;
import ru.babay.lib.Settings;
import ru.babay.lib.model.Image;
import ru.babay.lib.util.CacheCleanup;
import ru.babay.lib.util.DataWorkerThreads;
import ru.babay.lib.util.Util;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 12.12.12
 * Time: 20:57
 */
public class ImageCache {

    private static LruCache<String, Bitmap> mMemoryCache;
    private static final Object mMemCacheSync = new Object();
    static OutOfStorageListener outOfStorageListener;

    private static boolean isImageCacheExternal = false;

    public static void setImageCacheExternal(boolean imageCacheExternal) {
        isImageCacheExternal = imageCacheExternal;
    }

    public static void setOutOfStorageListener(OutOfStorageListener outOfStorageListener) {
        ImageCache.outOfStorageListener = outOfStorageListener;
    }

    static void initMemCache() {
        synchronized (mMemCacheSync) {
            if (mMemoryCache != null)
                return;

            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;

            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    if (Build.VERSION.SDK_INT < 19)
                        // The cache size will be measured in kilobytes rather than
                        // number of items.
                        return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                    try {
                        return bitmap.getAllocationByteCount() / 1024;
                    } catch (Exception e){
                        return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                    }
                }
            };
        }
    }

    public static Bitmap getBitmapFromMemCache(String url, LoadImageParams loadParams) {
        if (!Settings.MEMCACHE_ENABLED)
            return null;
        if (loadParams == null)
            return getBitmapFromMemCache(url, 0, 0);
        else
            return getBitmapFromMemCache(url, loadParams.maxWidth, loadParams.maxHeight);
    }

    public static Bitmap getBitmapFromMemCache(String url, int maxW, int maxH) {
        if (mMemoryCache == null)
            initMemCache();
        String key = getMemCacheKey(url, maxW, maxH);
        Bitmap b;
        synchronized (mMemCacheSync) {
            b = mMemoryCache.get(key);
        }
        if (b != null)
            return b;
        if (maxW == 0 && maxH == 0)
            return null;
        synchronized (mMemCacheSync) {
            b = mMemoryCache.get(getMemCacheKey(url, 0, 0));
        }
        if (b == null)
            return null;
        if (maxH == 0)
            maxH = Integer.MAX_VALUE;
        if (maxW == 0)
            maxW = Integer.MAX_VALUE;
        if (b.getWidth() <= maxW && b.getHeight() <= maxH)
            return b;
        b = Util.resizeBitmap(b, maxW, maxH, 0);
        addBitmapToMemoryCache(key, b);
        return b;
    }

    public static void cleanupImageCacheDir(Context context) {
        if (isImageCacheExternal)
            Util.cleanupImageCacheDir(Util.getExternalStoragePath(context));
        else
            Util.cleanupImageCacheDir(Util.getInternalStoragePath(context));
    }

    public static boolean cleanupOldImages(Context context, long lastCleanup, long cleanupInterval) {
        if (System.currentTimeMillis() - lastCleanup > cleanupInterval) {
            CacheCleanup task = new CacheCleanup(context, isImageCacheExternal);
            DataWorkerThreads.postToImageDownloadThreads(task);
            return true;
        }
        return false;
    }

    public static void addBitmapToMemoryCache(String url, Bitmap bitmap, int maxW, int maxH) {
        addBitmapToMemoryCache(getMemCacheKey(url, maxW, maxH), bitmap);
    }

    public static void addBitmapToMemoryCache(String url, Bitmap bitmap, LoadImageParams loadParams) {
        if (!Settings.MEMCACHE_ENABLED)
            return;

        if (loadParams == null)
            addBitmapToMemoryCache(getMemCacheKey(url, 0, 0), bitmap);
        else
            addBitmapToMemoryCache(getMemCacheKey(url, loadParams.maxWidth, loadParams.maxHeight), bitmap);
    }

    static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (mMemoryCache == null)
            initMemCache();
        if (key == null || bitmap == null)
            return;
        synchronized (mMemCacheSync) {
            if (mMemoryCache.get(key) == null) {
                mMemoryCache.put(key, bitmap);
            }
        }
    }

    static String getMemCacheKey(String url, int maxW, int maxH) {
        if (maxH == 0 && maxW == 0)
            return url;
        StringBuilder builder = new StringBuilder();
        builder.append(url);
        if (maxW != 0) {
            builder.append("?w");
            builder.append(Integer.toString(maxW));
        }
        if (maxH != 0) {
            builder.append("?h");
            builder.append(Integer.toString(maxH));
        }
        return builder.toString();
    }



    public static Bitmap getImage(Context context, String url, TTL ttl) {
        return getImage(context, url, ttl, null, isImageCacheExternal);
    }

    public static Bitmap getImage(Context context, Image image) {
        return getImage(context, image.getUrl(), image.getTtl(), null, isImageCacheExternal);
    }

    public static Bitmap getImage(Context context, Image image, LoadImageParams params) {
        return getImage(context, image.getUrl(), image.getTtl(), params, isImageCacheExternal);
    }

    public static Bitmap getImage(Context context, String url, TTL ttl, boolean isExternalStorage) {
        return getImage(context, url, ttl, null, isExternalStorage);
    }

    public static Bitmap getImage(Context context, String url, TTL ttl, int maxWidth) {
        return getImage(context, url, ttl, new LoadImageParams(maxWidth, 0), isImageCacheExternal);
    }

    public static Bitmap getImage(Context context, String url, TTL ttl, LoadImageParams loadParams, boolean isExternalStorage) {
        if (url == null)
            return null;

        Bitmap b = getBitmapFromMemCache(url, loadParams);
        if (b != null)
            return b;

        File file = CachedFile.getFile(context, getFilePath(Util.md5(url), ttl), isExternalStorage);
        if (file == null)
            return null;

        try {
            b = Util.getResizedBitmap(file, loadParams == null ? 0 : loadParams.maxWidth, loadParams == null ? 0 : loadParams.maxHeight);
            addBitmapToMemoryCache(url, b, loadParams);
            return b;
        } catch (IOException e) {
            BugHandler.logD(e);
            return null;
        }
    }


    public static void clear(Context context, Image image) {
        File file = CachedFile.getFile(context, getFilePath(Util.md5(image.getUrl()), image.getTtl()), isImageCacheExternal);

        if (file != null)
            file.delete();
    }

    public static void clear(Context context, String url, TTL ttl) {
        File file = CachedFile.getFile(context, getFilePath(Util.md5(url), ttl), isImageCacheExternal);

        if (file != null)
            file.delete();
    }

    public static String getPath(Context context, Image image) {
        File file = Util.makePathAndGetFile(context, getFilePath(Util.md5(image.getUrl()), image.getTtl()), isImageCacheExternal);
        //File file = CachedFile.getFile(context, getFilePath(Util.md5(image.getUrl()), image.getTtl()), isImageCacheExternal);
        return file.getPath();
    }

    public static boolean isCached(Context context, String url, TTL ttl) {
        return isCached(context, url, ttl, isImageCacheExternal);
    }

    public static boolean isCached(Context context, Image image) {
        return isCached(context, image, isImageCacheExternal);
    }

    public static boolean isCached(Context context, String url, TTL ttl, boolean isExternalStorage) {
        if (url == null)
            return false;

        File file = CachedFile.getFile(context, getFilePath(Util.md5(url), ttl), isExternalStorage);
        return file != null;
    }

    public static boolean isCached(Context context, Image image, boolean isExternalStorage) {
        if (image == null || image.getUrl() == null)
            return false;

        File file = CachedFile.getFile(context, getFilePath(image), isExternalStorage);
        return file != null;
    }

    public static CachedFile loadImage(Context context, Image image, LoadImageParams params, BitmapReceiver receiver) {
        return loadImage(context, image.getUrl(), image.getTtl(), params, true, isImageCacheExternal, receiver);
    }

    public static CachedFile loadImage(Context context, Image image, LoadImageParams params, boolean detectBlack, BitmapReceiver receiver) {
        return loadImage(context, image.getUrl(), image.getTtl(), params, detectBlack, isImageCacheExternal, receiver);
    }

    public static CachedFile loadImage(Context context, String url, TTL ttl, LoadImageParams params, boolean detectBlack, boolean isExternal, BitmapReceiver receiver) {
        if (url == null || url.length() == 0) {
            receiver.onFail(new Exception("no image to load"), null);
            return null;
        }

        CachedImageListener listener = new CachedImageListener(url, params, receiver);
        listener.ttl = ttl;
        listener.context = context;

        CachedFile downloader = new CachedFile(context, getFilePath(Util.md5(url), ttl), url, ttl, listener);
        listener.downloader = downloader;
        listener.detectBlack = detectBlack;
        downloader.setUseExternalStorage(isExternal);
        download(listener, 0);
        return downloader;
    }

    static void download(CachedImageListener listener, int delay) {
        if (isCached(listener.context, listener.url, listener.ttl))
            DataWorkerThreads.postToImageLoadThread(listener.downloader, delay);
        else
            DataWorkerThreads.postToImageDownloadThreads(listener.downloader, delay);
    }

    public static CachedFile loadImage(Context context, String url, TTL ttl, int maxWidth, boolean isExternal, BitmapReceiver receiver) {
        return loadImage(context, url, ttl, new LoadImageParams(maxWidth, 0), true, isExternal, receiver);
    }

    public static CachedFile loadImage(Context context, String url, TTL ttl, int maxWidth, BitmapReceiver receiver) {
        return loadImage(context, url, ttl, new LoadImageParams(maxWidth, 0), true, isImageCacheExternal, receiver);
    }

    public static Point getImageSize(Context context, Image image) {
        return getImageSize(context, image, isImageCacheExternal);
    }

    public static Point getImageSize(Context context, String url, TTL ttl, boolean isExternal) {
        String path = getFilePath(Util.md5(url), ttl);
        File file = Util.makePathAndGetFile(context, path, isExternal);
        if (!file.exists())
            return null;

        try {
            InputStream inputStream = new FileInputStream(file);
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, bitmapOptions);
            int imageWidth = bitmapOptions.outWidth;
            int imageHeight = bitmapOptions.outHeight;
            inputStream.close();
            return new Point(imageWidth, imageHeight);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public static Point getImageSize(Context context, String src, TTL ttl) {
        return getImageSize(context, new Image(src, ttl));
    }

    public static Point getImageSize(Context context, Image image, boolean isExternal) {
        if (image == null || image.getUrl() == null)
            return null;
        String path = getFilePath(image);
        File file = Util.makePathAndGetFile(context, path, isExternal);
        if (!file.exists())
            return null;

        try {
            InputStream inputStream = new FileInputStream(file);
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, bitmapOptions);
            int imageWidth = bitmapOptions.outWidth;
            int imageHeight = bitmapOptions.outHeight;
            inputStream.close();
            return new Point(imageWidth, imageHeight);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }

    private static class CachedImageListener implements CachedFile.CachedFileListener {
        BitmapReceiver receiver;
        String url;
        LoadImageParams params;
        TTL ttl;
        CachedFile downloader;
        Context context;
        int retryCount = 0;
        boolean detectBlack = true;

        private CachedImageListener(String url, LoadImageParams params, BitmapReceiver receiver) {
            this.url = url;
            this.params = params;
            this.receiver = receiver;
        }

        @Override
        public void onGotFile(File file, CachedFile cachedFile) {
            if (!cachedFile.isAborted())
                receivedFile(file, cachedFile);
        }

        @Override
        public void onUpdateFile(File file, CachedFile cachedFile) {
            if (!cachedFile.isAborted())
                receivedFile(file, cachedFile);
        }

        @Override
        public void onError(Throwable t, CachedFile cachedFile) {
            if (!cachedFile.isAborted())
                receiver.onFail(t, downloader);
            if (t instanceof IOException){
                String message = t.getMessage();
                if (outOfStorageListener != null && message != null && message.contains("space"))
                    outOfStorageListener.onOutOfStorage(context);
            }
        }

        void receivedFile(File file, CachedFile cachedFile) {
            if (receiver != null)
                try {
                    Bitmap bm = Util.getResizedBitmap(file, params == null ? 0 : params.maxWidth, params == null ? 0 : params.maxHeight);
                    if (cachedFile.isAborted())
                        return;
                    if (detectBlack && detectBlack(bm)) {
                        BugHandler.logD(String.format("Black image loaded, url: %s, path: %s, retryCount: %d", url, file.getPath(), retryCount));

                        if (retryCount == 0 || retryCount == 2) {
                            retryCount = 1;
                            download(this, 1000);
                        } else if (retryCount == 1) {
                            retryCount = 2;
                            ImageCache.clear(context, url, ttl);
                            download(this, 100);
                            //DataWorkerThreads.postToImageDownloadThreads(downloader);
                        } else {
                            receiver.onBitmapReceived(bm, downloader);
                        }
                    } else {
                        addBitmapToMemoryCache(url, bm, params);
                        receiver.onBitmapReceived(bm, downloader);
                    }
                } catch (IOException e) {
                    BugHandler.logW(e);
                    receiver.onFail(e, downloader);
                } catch (OutOfMemoryError e) {
                    BugHandler.logW(e);
                    receiver.onFail(e, downloader);
                }
        }
    }

    static Bitmap limitBitmapToWidth(Bitmap bmIn, int maxWidth) {
        if (bmIn != null && maxWidth > 0 && bmIn.getWidth() > maxWidth) {
            int newWidth = maxWidth;
            int newHeight = (int) ((newWidth / (float) bmIn.getWidth()) * bmIn.getHeight());
            return Bitmap.createScaledBitmap(bmIn, newWidth, newHeight, true);
        }
        return bmIn;
    }

    public static int getAverageColor(Bitmap bitmap, int step) {
        long redBucket = 0;
        long greenBucket = 0;
        long blueBucket = 0;
        long pixelCount = 0;

        for (int y = 0; y < bitmap.getHeight(); y += step) {
            for (int x = 0; x < bitmap.getWidth(); x += step) {
                int c = bitmap.getPixel(x, y);

                pixelCount++;
                redBucket += Color.red(c);
                greenBucket += Color.green(c);
                blueBucket += Color.blue(c);
                // does alpha matter?
            }
        }

        return Color.rgb((int) (redBucket / pixelCount),
                (int) (greenBucket / pixelCount),
                (int) (blueBucket / pixelCount));
    }

    public static boolean detectBlack(Bitmap b) {
        if (b == null)
            return true;
        int d = Math.min(b.getWidth(), b.getHeight());
        int step = d / 5;
        if (isBlack(getAverageColor(b, step))) {
            step = step / 2 - 1;
            return isBlack(getAverageColor(b, step));
        } else return false;
    }

    public static boolean isBlack(int c) {
        return Color.red(c) == 0 && Color.green(c) == 0 && Color.blue(c) == 0;
    }

    protected static Bitmap getBitmap(File file) throws IOException {
        FileInputStream input = new FileInputStream(file);
        Bitmap image = BitmapFactory.decodeStream(input);
        input.close();
        return image;
    }

    protected static String getFilePath(String id, TTL ttl) {
        return String.format("%s/%s.jpg", getFolderFor(ttl), id);
    }

    protected static String getFilePath(Image image) {
        return String.format("%s/%s.jpg", getFolderFor(image.getTtl()), Util.md5(image.getUrl()));
    }

    public static String getFolderFor(TTL ttl) {
        switch (ttl) {
            case TwoDays:
                return "2daysImg";
            case Week:
                return "weekImg";
            case Day:
            default:
                return "1dayImg";
        }
    }

    public interface BitmapReceiver {
        void onBitmapReceived(Bitmap bm, CachedFile downloader);

        void onFail(Throwable e, CachedFile downloader);
    }

    public static class LoadImageParams {
        public int maxWidth;
        public int maxHeight;

        public LoadImageParams(int maxWidth, int maxHeight) {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }

        public LoadImageParams() {
        }
    }

    public interface OutOfStorageListener {
        void onOutOfStorage(Context context);
    }
}