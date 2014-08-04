package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 18.04.13
 * Time: 3:10
 */
public class VerticalSwypeListenerRelativeLayout extends RelativeLayout {

    VerticalSwypeListener mSwypeListener;
    boolean mSwypeMode;
    boolean mSwypeReady;
    VelocityTracker mVelocityTracker;
    protected ScrollView mScrollView;
    private ViewGroup mRoot;
    float touchY;
    boolean interceptTouch = true;

    public VerticalSwypeListenerRelativeLayout(Context context) {
        super(context);
    }

    public VerticalSwypeListenerRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSwypeListenerRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSwypeListener(VerticalSwypeListener swypeListener) {
        mSwypeListener = swypeListener;
    }

    public void setRoot(ViewGroup mRoot) {
        this.mRoot = mRoot;
    }

    public void setScrollView(ScrollView mScrollView) {
        this.mScrollView = mScrollView;
    }

    public ViewGroup getRoot() {
        return mRoot;
    }

    public void setInterceptTouch(boolean interceptTouch) {
        this.interceptTouch = interceptTouch;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mSwypeListener == null || ! interceptTouch)
            return super.onInterceptTouchEvent(ev);

        int scrollPos = mScrollView.getScrollY();
        int contentHeight = mRoot.getHeight();
        int height = mScrollView.getHeight();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSwypeReady = scrollPos + height >= contentHeight;
                if (mSwypeReady) {
                    touchY = ev.getY();
                    if (mVelocityTracker == null)
                        mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.clear();
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

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mSwypeListener != null && mVelocityTracker != null && mSwypeMode) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    mSwypeListener.onSwypeRelease(mScrollView, mVelocityTracker.getYVelocity());
                }

                mSwypeMode = false;
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mSwypeListener == null || ! interceptTouch)
            return super.onTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dy = touchY - ev.getY();

                mVelocityTracker.addMovement(ev);
                touchY = ev.getY();
                if (mSwypeListener != null)
                    mSwypeListener.onSwypeBy(mScrollView, dy);
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mSwypeListener != null && mVelocityTracker != null && mSwypeMode) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    mSwypeListener.onSwypeRelease(mScrollView, mVelocityTracker.getYVelocity());
                }
                mSwypeMode = false;
                return true;
        }
        return false;
    }
}
