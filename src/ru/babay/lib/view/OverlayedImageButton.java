package ru.babay.lib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import ru.babay.lib.R;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 17.01.13
 * Time: 23:57
 */
public class OverlayedImageButton extends ImageView {
    Drawable mDrawableLeft;
    Drawable mDrawableRight;
    Drawable mDrawableTop;
    Drawable mDrawableBottom;
    int mDrawableLeftSize;
    int mDrawableRightSize;
    int mDrawableTopSize;
    int mDrawableBottomSize;

    public OverlayedImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(attrs, defStyle);
    }

    public OverlayedImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayedImageButton(Context context) {
        super(context);
    }

    void parseAttributes(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OverlayedImageButton, defStyle, 0);

        mDrawableLeft = a.getDrawable(R.styleable.OverlayedImageButton_drawableLeft);

        if (a.hasValue(R.styleable.OverlayedImageButton_drawableLeftSize))
            mDrawableLeftSize = a.getDimensionPixelSize(R.styleable.OverlayedImageButton_drawableLeftSize, 0);
        else if (mDrawableLeft != null)
            mDrawableLeftSize = Math.max(mDrawableLeft.getMinimumWidth(), mDrawableLeft.getIntrinsicWidth());

        mDrawableTop = a.getDrawable(R.styleable.OverlayedImageButton_drawableTop);

        if (a.hasValue(R.styleable.OverlayedImageButton_drawableTopSize))
            mDrawableTopSize = a.getDimensionPixelSize(R.styleable.OverlayedImageButton_drawableTopSize, 0);
        else if (mDrawableTop != null)
            mDrawableTopSize = Math.max(mDrawableTop.getMinimumWidth(), mDrawableTop.getIntrinsicWidth());

        mDrawableRight = a.getDrawable(R.styleable.OverlayedImageButton_drawableRight);

        if (a.hasValue(R.styleable.OverlayedImageButton_drawableRightSize))
            mDrawableRightSize = a.getDimensionPixelSize(R.styleable.OverlayedImageButton_drawableRightSize, 0);
        else if (mDrawableRight != null)
            mDrawableRightSize = Math.max(mDrawableRight.getMinimumWidth(), mDrawableRight.getIntrinsicWidth());

        mDrawableBottom = a.getDrawable(R.styleable.OverlayedImageButton_drawableBottom);

        if (a.hasValue(R.styleable.OverlayedImageButton_drawableBottomSize))
            mDrawableBottomSize = a.getDimensionPixelSize(R.styleable.OverlayedImageButton_drawableBottomSize, 0);
        else if (mDrawableBottom != null)
            mDrawableBottomSize = Math.max(mDrawableBottom.getMinimumWidth(), mDrawableBottom.getIntrinsicWidth());

        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mDrawableLeft != null) {
            if (mDrawableLeftSize != 0) {
                mDrawableLeft.setBounds(0, 0, mDrawableLeftSize, getHeight());
            }
            mDrawableLeft.draw(canvas);
        }

        if (mDrawableTop != null) {
            if (mDrawableTopSize != 0) {
                mDrawableTop.setBounds(0, 0, getWidth(), mDrawableTopSize);
            }
            mDrawableTop.draw(canvas);
        }

        if (mDrawableRight != null) {
            canvas.save();
            if (mDrawableRightSize != 0) {
                mDrawableRight.setBounds(0, 0, mDrawableRightSize, getHeight());
            }
            canvas.translate(getWidth() - mDrawableRightSize, 0);
            mDrawableRight.draw(canvas);
            canvas.restore();
        }

        if (mDrawableBottom != null) {
            canvas.save();
            if (mDrawableBottomSize != 0) {
                mDrawableBottom.setBounds(0, 0, getWidth(), mDrawableBottomSize);
            }
            canvas.translate(0, getHeight() - mDrawableBottomSize);
            mDrawableTop.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] state = getDrawableState();
        if (mDrawableLeft != null && mDrawableLeft.isStateful())
            mDrawableLeft.setState(state);

        if (mDrawableTop != null && mDrawableTop.isStateful())
            mDrawableTop.setState(state);

        if (mDrawableRight != null && mDrawableRight.isStateful())
            mDrawableRight.setState(state);

        if (mDrawableBottom != null && mDrawableBottom.isStateful())
            mDrawableBottom.setState(state);
    }

    public void setDrawableLeft(Drawable drawableLeft) {
        this.mDrawableLeft = drawableLeft;
    }

    public void setDrawableRight(Drawable drawableRight) {
        this.mDrawableRight = drawableRight;
    }

    public void setDrawableTop(Drawable drawableTop) {
        this.mDrawableTop = drawableTop;
    }

    public void setDrawableBottom(Drawable drawableBottom) {
        this.mDrawableBottom = drawableBottom;
    }

    public void setDrawableLeftSize(int size) {
        this.mDrawableLeftSize = size;
    }

    public void setDrawableRightSize(int size) {
        this.mDrawableRightSize = size;
    }

    public void setDrawableTopSize(int size) {
        this.mDrawableTopSize = size;
    }

    public void setDrawableBottomSize(int size) {
        this.mDrawableBottomSize = size;
    }
}
