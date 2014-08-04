package ru.babay.lib.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
//import ru.babay.lib.imagezoom.ImageViewTouch;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 22.08.13
 * Time: 20:10
 */
public class ImageViewTouchViewPager extends ViewPager {

    private static final String TAG = "ImageViewTouchViewPager";
    public static final String VIEW_PAGER_OBJECT_TAG = "image#";

    private int previousPosition;

    private OnPageSelectedListener onPageSelectedListener;

    public ImageViewTouchViewPager(Context context) {
        super(context);
        init();
    }

    public ImageViewTouchViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnPageSelectedListener(OnPageSelectedListener listener) {
        onPageSelectedListener = listener;
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (!checkV && v instanceof ImageViewTouchViewPager){
            ImageViewTouchViewPager pager = (ImageViewTouchViewPager) v;
            if (pager.getChildCount() == 3)
                v = pager.getChildAt(1);
            else if (pager.getCurrentItem() == 0)
                v = pager.getChildAt(0);
            else v = pager.getChildAt(pager.getChildCount()-1);
        }
        if (v instanceof TouchImageView) {
            return ((TouchImageView) v).canScroll(dx);
        } else if (v instanceof RelativeLayout && ((RelativeLayout) v).getChildCount() > 0 && ((RelativeLayout) v).getChildAt(0) instanceof TouchImageView) {
            TouchImageView ivt = (TouchImageView) ((RelativeLayout) v).getChildAt(0);
            return ivt.canScroll(dx);
        } else {
            return false; //super.canScroll(v, checkV, dx, x, y);
        }
    }

    public interface OnPageSelectedListener {

        public void onPageSelected(int position);

    }

    private void init() {
        previousPosition = getCurrentItem();

        setOnPageChangeListener(new SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (onPageSelectedListener != null) {
                    onPageSelectedListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == SCROLL_STATE_SETTLING && previousPosition != getCurrentItem()) {
                    View v = findViewWithTag(VIEW_PAGER_OBJECT_TAG + getCurrentItem());
                    if (v instanceof TouchImageView)
                        ((TouchImageView) v).zoomTo(1);
                    else if (v instanceof RelativeLayout && ((RelativeLayout) v).getChildCount() > 0 && ((RelativeLayout) v).getChildAt(0) instanceof TouchImageView) {
                        TouchImageView ivt = (TouchImageView) ((RelativeLayout) v).getChildAt(0);
                        ivt.zoomTo(1);
                    }

                    previousPosition = getCurrentItem();
                }
            }
        });
    }
}