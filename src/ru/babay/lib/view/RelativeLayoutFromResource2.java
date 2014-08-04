package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import ru.babay.lib.util.FontUtil;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 26.10.12
 * Time: 4:13
 * To change this template use File | Settings | File Templates.
 */
public abstract class RelativeLayoutFromResource2 extends RelativeLayout {
    public RelativeLayoutFromResource2(Context context) {
        super(context);
    }

    public RelativeLayoutFromResource2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutFromResource2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected abstract void sharedConstructor(AttributeSet attrs);

    protected void setContent(int layoutId){
        removeAllViews();
        View v = inflate(getContext(), layoutId, null);
        if (v instanceof RelativeLayout)
            setContent((RelativeLayout) v);
        else
            setContent((ViewGroup) v);
    }

    protected void setContent(ViewGroup source){
        addView(source, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        source.setDuplicateParentStateEnabled(true);
    }

    protected void setContent(RelativeLayout source){
        View v;
        while (source.getChildCount() > 0) {
            v = source.getChildAt(0);
            source.removeView(v);
            addView(v);
        }
        if (source.getBackground() != null)
            setBackgroundDrawable(source.getBackground());
        setPadding(source.getPaddingLeft(), source.getPaddingTop(), source.getPaddingRight(), source.getPaddingRight());
    }
}
