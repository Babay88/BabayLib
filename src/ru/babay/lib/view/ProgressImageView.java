package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import ru.babay.lib.R;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 10.12.12
 * Time: 17:25
 * To change this template use File | Settings | File Templates.
 */
public class ProgressImageView extends ImageView {
    private static final int PERIOD = 1000;
    private static final int STEPS = 8;
    boolean running;
    private static LinearInterpolator interpolator = new LinearInterpolator();

    public ProgressImageView(Context context) {
        super(context);
        sharedConstructor(context);
    }

    public ProgressImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructor(context);
    }

    public ProgressImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sharedConstructor(context);
    }

    void sharedConstructor(Context context) {
        setImageResource(R.drawable.progress_image);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            startAnimation();
        } else stopAnimation();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            startAnimation();
        } else stopAnimation();
    }

    public void startAnimation() {
        if (running)
            return;
        running = true;
        /*StepRotateAnimation animation = new StepRotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setSteps(STEPS);*/
        int x = getDrawable().getIntrinsicWidth()/2;
        int y = getDrawable().getIntrinsicHeight()/2;

        Animation animation = new RotateAnimation(0, 360, Animation.ABSOLUTE, x, Animation.ABSOLUTE, y);
        animation.setDuration(PERIOD);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setInterpolator(interpolator);
        startAnimation(animation);
    }

    public void stopAnimation() {
        clearAnimation();
        running = false;
    }

    public RelativeLayout makeLayout(){
        return new ProgressImageLayout(this);
    }

    public RelativeLayout makeCenteredLayout(int centerToId){
        RelativeLayout layout = new ProgressImageLayout(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        lp.addRule(RelativeLayout.ALIGN_LEFT, centerToId);
        lp.addRule(RelativeLayout.ALIGN_TOP, centerToId);
        lp.addRule(RelativeLayout.ALIGN_RIGHT, centerToId);
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, centerToId);
        layout.setLayoutParams(lp);
        return layout;
    }

}
