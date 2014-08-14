package ru.babay.lib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;
import ru.babay.lib.R;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 18.01.13
 * Time: 9:32
 */
public class OverlayedTextView extends TextView {

    Drawable mLeftOverlay;
    Drawable mRightOverlay;

    public OverlayedTextView(Context context) {
        super(context);
    }

    public OverlayedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(attrs, defStyle);
    }

    void parseAttributes(AttributeSet attrs, int defStyle){
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OverlayedTextView, defStyle, 0);

        mLeftOverlay = a.getDrawable(R.styleable.OverlayedTextView_drawableLeft);
        mRightOverlay = a.getDrawable(R.styleable.OverlayedTextView_drawableRight);

        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mLeftOverlay != null) {
            mLeftOverlay.setBounds(0, 0, mLeftOverlay.getIntrinsicWidth(), getHeight());
            mLeftOverlay.draw(canvas);
        }

        if (mRightOverlay != null) {
            canvas.save();
            mRightOverlay.setBounds(0, 0, mRightOverlay.getIntrinsicWidth(), getHeight());
            canvas.translate(getWidth() - mRightOverlay.getIntrinsicWidth(), 0);
            mRightOverlay.draw(canvas);
            canvas.restore();
        }
    }

    public void setLeftOverlay(Drawable drawable) {
        this.mLeftOverlay = drawable;
    }

    public void setRightOverlay(Drawable drawable) {
        this.mRightOverlay = drawable;
    }
}
