package ru.babay.lib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import ru.babay.lib.BugHandler;
import ru.babay.lib.R;
import ru.babay.lib.Settings;
import ru.babay.lib.transport.CachedFile;
import ru.babay.lib.transport.ImageCache;
import ru.babay.lib.transport.TTL;
import ru.babay.lib.model.Image;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 06.08.13
 * Time: 2:40
 */
public class LoadableImageView extends ImageView {
    protected Object sourceItem;
    protected int loaderId;
    protected int defDrawableId;
    protected boolean download;
    protected boolean loadFired;
    protected int bmWidth, bmHeight;
    protected boolean fitWidth;
    protected int maxImageHeight;
    protected boolean cropHeight;
    protected Image mImage;
    protected OnImageLoadedListener onImageLoadedListener;
    protected View loaderView;
    protected Drawable overlayDrawable;
    int loadingLimitMult;
    int retryCount = 0;
    boolean fadeIn;
    CachedFile imageDownloadHandler;


    public LoadableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.LoadableImageView, defStyle, 0);
        if (arr.hasValue(R.styleable.LoadableImageView_overlayDrawable))
            overlayDrawable = arr.getDrawable(R.styleable.LoadableImageView_overlayDrawable);

        arr.recycle();
    }

    public LoadableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadableImageView(Context context) {
        super(context);
    }


    public boolean isCached() {
        return ImageCache.isCached(getContext(), mImage);
    }

    public Point getCachedImageSize() {
        return ImageCache.getImageSize(getContext(), mImage);
    }

    public void clearImage() {
        abortDownload();
        /*Drawable dr = getDrawable();
        if (dr instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) dr).getBitmap();
            if (b != null)
                b.recycle();
        } */
        setImageDrawable(null);
    }

    void abortDownload() {
        if (imageDownloadHandler != null) {
            imageDownloadHandler.abort();
            imageDownloadHandler = null;
            hideProgress(loaderId, loaderView);
        }
    }

    public void setImageUrl(Object sourceItem, String url, TTL ttl) {
        abortDownload();
        this.sourceItem = sourceItem;
        mImage = new Image(url, 0, 0, ttl);
        loadFired = false;
    }

    public void setImage(Object sourceItem, Image image) {
        abortDownload();
        this.sourceItem = sourceItem;
        mImage = image;
        loadFired = false;
        fitWidth = false;
        bmWidth = bmHeight = 0;
    }

    public Image getImage() {
        return mImage;
    }

    public void setFitWidth(boolean fitWidth) {
        this.fitWidth = fitWidth;
        if (fitWidth) {
            fillBmSize();
        }
    }

    public void setMaxImageHeight(int maxImageHeight) {
        this.maxImageHeight = maxImageHeight;
        if (maxImageHeight > 0) {
            fillBmSize();
        }
    }

    public void setCropHeight(boolean cropHeight) {
        this.cropHeight = cropHeight;
        if (cropHeight) {
            fillBmSize();
        }
    }

    void fillBmSize() {
        if (mImage.getWidth() != 0 && mImage.getHeight() != 0) {
            bmWidth = mImage.getWidth();
            bmHeight = mImage.getHeight();
        } else {
            Point pt = ImageCache.getImageSize(getContext(), mImage);
            if (pt != null) {
                bmWidth = pt.x;
                bmHeight = pt.y;
            }
        }
    }

    /*public void setLimitHeightWithWidth(boolean limitHeightWithWidth) {
        this.limitHeightWithWidth = limitHeightWithWidth;
        if (limitHeightWithWidth){
            if (mImage.getWidth() != 0 && mImage.getHeight() != 0) {
                bmWidth = mImage.getWidth();
                bmHeight = mImage.getHeight();
            } else {
                Point pt = ImageCache.getImageSize(getContext(), mImage);
                if (pt != null) {
                    bmWidth = pt.x;
                    bmHeight = pt.y;
                }
            }
        }
    } */

    /*public void loadImage() {
        ViewGroup.LayoutParams lp = getLayoutParams();
        ImageCache.LoadImageParams loadParams = new ImageCache.LoadImageParams();
        if (lp != null) {
            if (lp.width > 0)
                loadParams.maxWidth = lp.width;
            if (lp.height > 0)
                loadParams.maxHeight = lp.height;
        }

        loadImage(loadParams);
    } */

    public void loadImage(int limitMult) {
        retryCount = 0;
        this.loadingLimitMult = limitMult;
        ImageCache.LoadImageParams loadParams = new ImageCache.LoadImageParams();

        if (limitMult > 0) {
            ViewGroup.LayoutParams lp = getLayoutParams();

            if (lp != null && (lp.width > 0 || lp.height > 0)) {
                if (lp.width > 0)
                    loadParams.maxWidth = lp.width / limitMult;
                if (lp.height > 0)
                    loadParams.maxHeight = lp.height / limitMult;
            } else {
                if (getWidth() > 0)
                    loadParams.maxWidth = getWidth() / limitMult;
            }
        }

        loadImage(loadParams);
    }

    public void loadImage(boolean limitSize) {
        loadImage(limitSize ? 1 : 0);
    }


    public void loadImageifNotLoaded(boolean limit) {
        if (!loadFired)
            loadImage(limit);
    }

    public void setLoadParams(int loaderId, int defDrawableId, boolean download) {
        this.loaderId = loaderId;
        this.defDrawableId = defDrawableId;
        this.download = download;
    }

    public void loadPreview(int maxWidth, int maxheight) {
        loadImage(new ImageCache.LoadImageParams(maxWidth, maxheight));
        loadFired = false;
    }

    void loadImage(ImageCache.LoadImageParams loadParams) {
        loadFired = true;

        if (mImage == null || mImage.getUrl() == null || mImage.getUrl().length() == 0) {
            hideProgress(loaderId, loaderView);
            setDefImage(defDrawableId);
            return;
        }

        Bitmap b = ImageCache.getBitmapFromMemCache(mImage.getUrl(), loadParams.maxWidth, loadParams.maxHeight);
        if (b != null) {
            setImageBitmap(b);
            return;
        }

        if (getDrawable() == null)
            showLoader(loaderId);
        //setImageDrawable(null);

        if (download || isCached()) {
            //showLoader(loaderId);
            ImageReceiver receiver = new ImageReceiver(sourceItem, loaderId, defDrawableId);
            imageDownloadHandler = ImageCache.loadImage(getContext(), mImage, loadParams, false, receiver);
        } else {
            hideProgress(loaderId, null);
            setDefImage(defDrawableId);
        }
    }

    void setDefImage(int defDrawableId) {
        if (defDrawableId == 0)
            setVisibility(View.GONE);
        else
            setImageResource(defDrawableId);
    }

    public void hideProgress(int progressId, View progressView) {
        setVisibility(View.VISIBLE);
        if (progressId == 0) {
            if (progressView != null)
                ((ViewGroup) getParent()).removeView(progressView);
        } else {
            if (progressView == null)
                progressView = ((ViewGroup) getParent()).findViewById(progressId);
            if (progressView != null)
                progressView.setVisibility(View.GONE);
        }
        loaderView = null;
    }

    public View showLoader(int id) {
        if (loaderView != null)
            return loaderView;

        ViewParent parent = getParent();

        //setVisibility(INVISIBLE);

        if (id != 0) {
            View progress = ((ViewGroup) parent).findViewById(id);
            progress.setVisibility(View.VISIBLE);
            if (parent instanceof LinearLayout)
                setVisibility(View.GONE);
            //else if (parent instanceof RelativeLayout)
            //    view.setVisibility(INVISIBLE);
            return loaderView = progress;
        }

        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams loaderLP = new LinearLayout.LayoutParams(getLayoutParams().width, getLayoutParams().height);
            setVisibility(View.GONE);
            ProgressImageLayout progress = new ProgressImageLayout(getContext());
            int pos = ((LinearLayout) parent).indexOfChild(this);
            ((LinearLayout) parent).addView(progress, pos, loaderLP);
            return loaderView = progress;
        } else if (parent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
            int imageId = getId();
            lp.addRule(RelativeLayout.ALIGN_LEFT, imageId);
            lp.addRule(RelativeLayout.ALIGN_RIGHT, imageId);
            lp.addRule(RelativeLayout.ALIGN_TOP, imageId);
            lp.addRule(RelativeLayout.ALIGN_BOTTOM, imageId);

            ProgressImageLayout progress = new ProgressImageLayout(getContext());
            ((RelativeLayout) parent).addView(progress, lp);
            //setVisibility(INVISIBLE);
            return loaderView = progress;
        }
        return null;
    }

    public void setOnImageLoadedListener(OnImageLoadedListener onImageLoadedListener) {
        this.onImageLoadedListener = onImageLoadedListener;
    }

    void fadeIn() {
        Animation anim = new AlphaAnimation(0, 1);
        anim.setDuration(300);
        anim.setFillBefore(true);
        startAnimation(anim);
    }

    private class ImageReceiver implements ImageCache.BitmapReceiver {
        Object item;
        android.widget.ImageView view;
        int loaderId;
        int defDrawableId;
        Handler handler = new Handler();

        private ImageReceiver(Object item, int loaderId, int defDrawableId) {
            this.item = item;
            this.loaderId = loaderId;
            this.defDrawableId = defDrawableId;
        }

        @Override
        public void onBitmapReceived(final Bitmap bm) {
            imageDownloadHandler = null;
            if (item == sourceItem) {
                if (ImageCache.detectBlack(bm)) {
                    if (retryCount == 0) {
                        BugHandler.logD(String.format("image loaded as black, url: %s, path: %s",
                                mImage.getUrl(), ImageCache.getPath(getContext(), mImage)));
                        postLoadImage(1000, 1);
                    } else if (retryCount == 1) {
                        ImageCache.clear(getContext(), mImage);
                        postLoadImage(100, 1);

                    }


                } else
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            hideProgress(loaderId, loaderView);
                            setImageBitmap(bm);
                            bmWidth = bm.getWidth();
                            bmHeight = bm.getHeight();
                            if (onImageLoadedListener != null)
                                onImageLoadedListener.onImageLoaded(LoadableImageView.this);
                            if (fadeIn)
                                fadeIn();
                        }
                    });

            } else postHideProgress(loaderId, loaderView);
        }

        @Override
        public void onFail(Throwable e) {
            imageDownloadHandler = null;
            if (item == sourceItem)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress(loaderId, loaderView);
                        setDefImage(defDrawableId);
                        if (onImageLoadedListener != null)
                            onImageLoadedListener.onImageLoadFailed(LoadableImageView.this);
                    }
                });
            else postHideProgress(loaderId, loaderView);
        }


        void postLoadImage(int delay, final int setRetryCount) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadImage(loadingLimitMult);
                    retryCount = setRetryCount;
                }
            }, delay);
        }
    }

    void postHideProgress(final int loaderId, final View loaderView) {
        post(new Runnable() {
            @Override
            public void run() {
                hideProgress(loaderId, loaderView);
            }
        });
    }

   /* @Override
    protected void onDetachedFromWindow() {
        abortDownload();
        super.onDetachedFromWindow();
    }*/

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int hMode = View.MeasureSpec.getMode(heightMeasureSpec);
        if (hMode == View.MeasureSpec.EXACTLY)
            return;

        final int hSize = View.MeasureSpec.getSize(heightMeasureSpec);
        final int width = getMeasuredWidth();

        int h = 0;
        int w = width;
        if (fitWidth && bmHeight != 0 && bmWidth != 0 && getDrawable() == null) {
            h = width * bmHeight / bmWidth;
            w = width;
        }
        if (maxImageHeight > 0) {
            if (bmHeight != 0 && bmWidth != 0) {
                h = width * bmHeight / bmWidth;

                if (h > maxImageHeight) {
                    h = maxImageHeight;
                    w = h * bmWidth / bmHeight;
                }
            }
        }
        if (cropHeight) {
            if (bmHeight > bmWidth) {
                h = width * 2 / 3;
                setScaleType(ScaleType.CENTER_CROP);
            } else h = Math.round(width * bmHeight / (float) bmWidth);
            if (hMode == View.MeasureSpec.AT_MOST) {
                h = Math.min(h, hSize);
            }
        }

        if (h != 0) {
            if (hMode == View.MeasureSpec.AT_MOST)
                h = Math.min(h, hSize);
            if (h != getMeasuredHeight())
                setMeasuredDimension(w, h);
        }

    }

    public void setOverlayDrawable(Drawable overlayDrawable) {
        this.overlayDrawable = overlayDrawable;
    }

    public void setFadeIn(boolean fadeIn) {
        this.fadeIn = fadeIn;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (overlayDrawable != null) {
            overlayDrawable.setBounds(0, 0, getWidth(), getHeight());
            overlayDrawable.draw(canvas);
        }

    }

    public interface OnImageLoadedListener {
        public void onImageLoaded(LoadableImageView view);

        public void onImageLoadFailed(LoadableImageView view);
    }
}
