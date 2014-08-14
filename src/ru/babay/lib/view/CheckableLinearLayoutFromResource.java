package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 07.12.12
 * Time: 18:49
 * To change this template use File | Settings | File Templates.
 */
public abstract class CheckableLinearLayoutFromResource extends LinearLayoutFromResource implements Checkable {
    boolean mChecked = false;
    OnCheckedChangeListener checkedChangeListener;

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    public CheckableLinearLayoutFromResource(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected CheckableLinearLayoutFromResource(Context context) {
        super(context);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
        }
        if (checkedChangeListener != null)
            checkedChangeListener.onCheckedChanged(this, mChecked);
    }

    @Override
    public void toggle() {
        mChecked = !mChecked;
        refreshDrawableState();
        if (checkedChangeListener != null)
            checkedChangeListener.onCheckedChanged(this, mChecked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    public void setCheckedChangeListener(OnCheckedChangeListener checkedChangeListener) {
        this.checkedChangeListener = checkedChangeListener;
    }

    public static interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        void onCheckedChanged(CheckableLinearLayoutFromResource buttonView, boolean isChecked);
    }

}