package ru.babay.lib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import ru.babay.lib.R;
import ru.babay.lib.transport.ImageCache;
import ru.babay.lib.transport.TTL;

public class MyImageGetter implements Html.ImageGetter {
    Context mContext;
    ImageListener mListener;

    public MyImageGetter(Context context, ImageListener mListener) {
        mContext = context;
        this.mListener = mListener;
    }

    int toLoad = 0;

    public Drawable getDrawable(String source) {
        Bitmap bm = ImageCache.getImage(mContext, source, TTL.Day);
        if (bm != null) {
            float sizeMult = mContext.getResources().getDisplayMetrics().density * 1.5f;
            Drawable d = new BitmapDrawable(bm);
            d.setBounds(0, 0, (int) (sizeMult * d.getIntrinsicWidth()), (int) (sizeMult * d.getIntrinsicHeight()));
            return d;
        }
        /*if (UserPreferences.getInstance(mContext.getApplicationContext()).shouldLoadImages()) {
            toLoad++;
            ImageCache.loadImage(mContext, source, ImageCache.TTL.Day, true, imageReceiver);
        } */

        Drawable d = mContext.getResources().getDrawable(R.drawable.empty);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        return d;

    }

    public interface ImageListener {
        public void onBitmapsReceived();
    }

    ImageCache.BitmapReceiver imageReceiver = new ImageCache.BitmapReceiver() {
        @Override
        public void onBitmapReceived(Bitmap bm) {
            toLoad--;
            if (toLoad == 0)
                mListener.onBitmapsReceived();
        }

        @Override
        public void onFail(Throwable e) {
            toLoad--;
            if (toLoad == 0)
                mListener.onBitmapsReceived();
        }
    };

}