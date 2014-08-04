package ru.babay.lib.view;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 20.08.13
 * Time: 23:31
 */
/*
 * TouchImageView.java
 * By: Michael Ortiz
 * Updated By: Patrick Lackemacher
 * -------------------
 * Extends Android ImageView to include pinch zooming and panning.
 */


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import ru.babay.lib.view.LoadableImageView;

public class TouchImageView extends LoadableImageView {

    Matrix matrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF last = new PointF();
    PointF start = new PointF();
    float minScale = 1f;
    float maxScale = 10f;
    float[] m;


    int width, height;
    static final int CLICK = 3;
    float saveScale = 1f;
    protected float origWidth, origHeight;
    int oldMeasuredWidth, oldMeasuredHeight;
    RectF bitmapRect = new RectF();
    RectF scaledRect = new RectF();



    ScaleGestureDetector mScaleDetector;


    Context context;

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        matrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);

        matrix.getValues(m);
        float x = m[Matrix.MTRANS_X];
        float y = m[Matrix.MTRANS_Y];
        PointF curr = new PointF(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                last.set(event.getX(), event.getY());
                start.set(last);
                mode = DRAG;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    float deltaX = curr.x - last.x;
                    float deltaY = curr.y - last.y;
                    matrix.postTranslate(deltaX, deltaY);
                    fixTrans();
                    last.set(curr.x, curr.y);
                }
                break;

            case MotionEvent.ACTION_UP:
                mode = NONE;
                int xDiff = (int) Math.abs(curr.x - start.x);
                int yDiff = (int) Math.abs(curr.y - start.y);
                if (xDiff < CLICK && yDiff < CLICK)
                    performClick();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
        setImageMatrix(matrix);
        invalidate();
        return true; // indicate event was handled

    }

    public void setMaxZoom(float x) {
        maxScale = x;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }

            if (origWidth * saveScale > width || origHeight * saveScale > height) {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
            } else {
                matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
            }

            fixTrans();
            return true;
        }
    }

    void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, width, origWidth * saveScale);
        float fixTransY = getFixTrans(transY, height, origHeight * saveScale);

        if (fixTransX != 0 || fixTransY != 0)
            matrix.postTranslate(fixTransX, fixTransY);
    }

    float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = maxTrans = (viewSize - contentSize)/2;
            //minTrans = 0;
            //maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        if (oldMeasuredHeight == width && oldMeasuredHeight == height
                || width == 0 || height == 0)
            return;
        oldMeasuredHeight = height;
        oldMeasuredWidth = width;

        if (saveScale == 1) {
            //Fit to screen.
            float scale;

            Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0)
                return;
            int bmWidth = drawable.getIntrinsicWidth();
            int bmHeight = drawable.getIntrinsicHeight();

            float scaleX = (float) width / (float) bmWidth;
            float scaleY = (float) height / (float) bmHeight;
            scale = Math.min(scaleX, scaleY);
            //minScale = scale;
            matrix.setScale(scale, scale);
            setImageMatrix(matrix);

            // Center the image
            float redundantYSpace = (float) height - (scale * (float) bmHeight);
            float redundantXSpace = (float) width - (scale * (float) bmWidth);
            redundantYSpace /= (float) 2;
            redundantXSpace /= (float) 2;

            matrix.postTranslate(redundantXSpace, redundantYSpace);

            origWidth = width - 2 * redundantXSpace;
            origHeight = height - 2 * redundantYSpace;
            setImageMatrix(matrix);
        }
        fixTrans();
    }

    public boolean canScroll( int direction ) {
        /*RectF bitmapRect = getBitmapRect();
        updateRect( bitmapRect, mScrollRect );
        Rect imageViewRect = new Rect();
        getGlobalVisibleRect( imageViewRect );

        if( null == bitmapRect ) {
            return false;
        }

        if ( bitmapRect.right >= imageViewRect.right ) {
            if ( direction < 0 ) {
                return Math.abs( bitmapRect.right - imageViewRect.right ) > SCROLL_DELTA_THRESHOLD;
            }
        }

        double bitmapScrollRectDelta = Math.abs( bitmapRect.left - mScrollRect.left );
        return bitmapScrollRectDelta > SCROLL_DELTA_THRESHOLD;*/
        RectF rectF = getBitmapRect();
        if (rectF == null)
            return false;

        scaledRect.set(rectF);
        matrix.mapRect(scaledRect);
        if (direction > 0)
            return scaledRect.left + direction < 0;
        else
            return scaledRect.right + direction > getWidth();
    }

    protected RectF getBitmapRect() {
        final Drawable drawable = getDrawable();

        if ( drawable == null ) return null;
        bitmapRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return bitmapRect;
    }

    public void zoomTo(float zoom){
        matrix.reset();
        matrix.setScale(zoom, zoom);
        setImageMatrix(matrix);
    }
}