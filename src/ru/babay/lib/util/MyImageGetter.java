package ru.babay.lib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import ru.babay.lib.R;
import ru.babay.lib.transport.CachedFile;
import ru.babay.lib.transport.ImageCache;
import ru.babay.lib.transport.TTL;

import java.util.HashMap;

public class MyImageGetter implements Html.ImageGetter {
    private static boolean isShouldLoadImages = true;

    Context mContext;
    ImageListener mListener;
    boolean downloadImages = true;
    int maxWidth;

    String defServer = "";

    public static void setIsShouldLoadImages(boolean isShouldLoadImages) {
        MyImageGetter.isShouldLoadImages = isShouldLoadImages;
    }

    public MyImageGetter(Context context, ImageListener mListener) {
        mContext = context;
        this.mListener = mListener;
    }

    volatile int toLoad = 0;

    public Drawable getDrawable(final String sourceIn) {
        if (sourceIn == null)
            return null;

        final String source = sourceIn.substring(0, 1).equals("/") ? defServer + sourceIn : sourceIn;

        Bitmap bm = ImageCache.getBitmapFromMemCache(source, maxWidth, 0);
        if (bm != null)
            return makeDrawableFromBitmap(mContext, bm, maxWidth);


        DataWorkerThreads.postToImageThreadsS(new Runnable() {
            @Override
            public void run() {
                toLoad++;
                Bitmap bm = ImageCache.getImage(mContext, source, TTL.Day, maxWidth);
                if (bm != null) {
                    toLoad--;
                    checkLoaded();
                    return;
                }
                toLoad--;

                if (downloadImages && isShouldLoadImages) {
                    toLoad++;
                    ImageCache.loadImage(mContext, source, TTL.Day, maxWidth, new ImageReceiver(source));
                } else
                    checkLoaded();
            }
        });


        Drawable d = mContext.getResources().getDrawable(R.drawable.empty);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        return d;
    }

    public static Drawable makeDrawableFromBitmap(Context context, Bitmap bm, int maxWidth) {
        float sizeMult = context.getResources().getDisplayMetrics().density * 1.5f;
        if (maxWidth != 0 && bm.getWidth() >= maxWidth / sizeMult)
            sizeMult = context.getResources().getDisplayMetrics().density;

        Drawable d = new BitmapDrawable(bm);
        d.setBounds(0, 0, (int) (sizeMult * d.getIntrinsicWidth()), (int) (sizeMult * d.getIntrinsicHeight()));
        return d;
    }

    public boolean isDownloadImages() {
        return downloadImages;
    }

    public void setDownloadImages(boolean downloadImages) {
        this.downloadImages = downloadImages;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public interface ImageListener {
        public void onBitmapsReceived();
    }

    void checkLoaded() {
        //if (toLoad == 0)
        mListener.onBitmapsReceived();
    }

    private class ImageReceiver implements ImageCache.BitmapReceiver {
        String source;

        private ImageReceiver(String source) {
            this.source = source;
        }

        @Override
        public void onBitmapReceived(Bitmap bm, CachedFile downloader) {
            toLoad--;
            checkLoaded();
        }

        @Override
        public void onFail(Throwable e, CachedFile downloader) {
            toLoad--;
            checkLoaded();
        }
    }

    public void setDefServer(String defServer) {
        this.defServer = defServer;
    }
}