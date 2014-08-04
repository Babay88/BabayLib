package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.ScrollView;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 12.08.13
 * Time: 13:09
 */
public class VerticalSwypeListenerScrollView extends ScrollView {

    VerticalSwypeListener mSwypeListener;
    boolean mSwypeMode;
    boolean mSwypeReady;
    VelocityTracker mVelocityTracker;
    float touchY;
    boolean interceptTouch = true;
    OnScrollYListener onScrollYListener;

    public VerticalSwypeListenerScrollView(Context context) {
        super(context);
    }

    public VerticalSwypeListenerScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSwypeListenerScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSwypeListener(VerticalSwypeListener swypeListener) {
        mSwypeListener = swypeListener;
    }

    public void setInterceptTouch(boolean interceptTouch) {
        this.interceptTouch = interceptTouch;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!interceptTouch)
            return false;
        if (mSwypeListener == null || getChildCount() < 1)
            return super.onInterceptTouchEvent(ev);

        int scrollPos = getScrollY();

        int contentHeight = getChildAt(0).getHeight();
        int height = getHeight();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSwypeReady = scrollPos + height >= contentHeight;
                //mSwypeReady =  height < contentHeight;
                if (mSwypeReady) {
                    try {
                        touchY = ev.getY();
                        if (mVelocityTracker == null)
                            mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.clear();
                    } catch (Exception e) {
                        int i = 0;
                        i++;
                        i++;
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (!mSwypeReady)
                    break;

                float dy = touchY - ev.getY();

                if (dy > 20) {
                    mSwypeMode = true;
                    touchY = ev.getY();
                    return true;
                } else if (dy < -20)
                    mSwypeReady = false;
                if (mSwypeMode)
                    return true;

                break;
            case MotionEvent.ACTION_UP:
                if (mSwypeListener != null && mVelocityTracker != null && mSwypeMode) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    mSwypeListener.onSwypeRelease(this, mVelocityTracker.getYVelocity());
                }

                mSwypeMode = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                mSwypeMode = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!interceptTouch)
            return false;
        if (mSwypeListener == null || !mSwypeReady)
            return super.onTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dy = touchY - ev.getY();

                mVelocityTracker.addMovement(ev);
                touchY = ev.getY();
                if (mSwypeListener != null){
                    if (!mSwypeMode)
                        mSwypeListener.onSwypeStart(this);
                    mSwypeListener.onSwypeBy(this, dy);
                }
                mSwypeMode = true;
                return true;

            case MotionEvent.ACTION_UP:
                if (mSwypeListener != null && mVelocityTracker != null && mSwypeMode) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    mSwypeListener.onSwypeRelease(this, mVelocityTracker.getYVelocity());
                }
                mSwypeMode = false;
                super.onTouchEvent(ev);
                return true;
            case MotionEvent.ACTION_CANCEL:
                mSwypeMode = false;
                return super.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (t != oldt && onScrollYListener != null)
            onScrollYListener.onScrollY(t, oldt);

    }

    public void setOnScrollYListener(OnScrollYListener onScrollYListener) {
        this.onScrollYListener = onScrollYListener;
    }

    public interface OnScrollYListener{
        public void onScrollY(int y, int oldY);
    }
}
