package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
* Created with IntelliJ IDEA.
* User: babay
* Date: 09.01.13
* Time: 2:21
* To change this template use File | Settings | File Templates.
*/
public class ProgressImageLayout extends RelativeLayout {
    ProgressImageView mImage;
    public ProgressImageLayout(Context context) {
        super(context);
        sharedConstructor();
    }

    public ProgressImageLayout(ProgressImageView mImage) {
        super(mImage.getContext());
        this.mImage = mImage;
        sharedConstructor();
    }

    public ProgressImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructor();
    }

    public ProgressImageLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sharedConstructor();
    }

    void sharedConstructor(){
        if (mImage == null)
            mImage = new ProgressImageView(getContext());

        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(CENTER_IN_PARENT);
        addView(mImage, layoutParams);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mImage.setVisibility(visibility);
    }
}
