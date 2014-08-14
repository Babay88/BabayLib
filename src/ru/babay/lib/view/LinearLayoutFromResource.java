package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 26.10.12
 * Time: 4:13
 * To change this template use File | Settings | File Templates.
 */
public abstract class LinearLayoutFromResource extends LinearLayout {
    public LinearLayoutFromResource(Context context) {
        super(context);
    }

    public LinearLayoutFromResource(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected abstract void sharedConstructor(AttributeSet attrs);

    protected void setContent(int resourceId){
        LinearLayout source = (LinearLayout)inflate(getContext(), resourceId, null);
        View v;
        while (source.getChildCount() > 0) {
            v = source.getChildAt(0);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            source.removeView(v);
            addView(v, lp);
        }
        if (source.getBackground() != null)
            setBackgroundDrawable(source.getBackground());
        setPadding(source.getPaddingLeft(), source.getPaddingTop(), source.getPaddingRight(), source.getPaddingRight());
        setOrientation(source.getOrientation());
    }

}
