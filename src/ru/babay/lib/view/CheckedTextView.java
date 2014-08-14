package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 28.01.13
 * Time: 17:13
 */
public class CheckedTextView extends android.widget.CheckedTextView {
    public CheckedTextView(Context context) {
        super(context);
    }

    public CheckedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }
}
