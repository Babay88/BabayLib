package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import ru.babay.lib.model.ItemBase;
import ru.babay.lib.model.ItemContainer;

/**
 * Created with IntelliJ IDEA.
 * User: Babay
 * Date: 12.06.13
 * Time: 18:48
 * To change this template use File | Settings | File Templates.
 */
public abstract class ItemContextMenuView extends LinearLayoutFromResource{
    protected ItemContainer.ItemListener itemClickListener;
    protected ItemBase mItem;
    protected ItemContainer mParent;

    public ItemContextMenuView(Context context, ItemBase item, ItemContainer parent, ItemContainer.ItemListener listener) {
        super(context);
        mItem = item;
        itemClickListener = listener;
        mParent = parent;
    }

    protected void onLike(){
        if (itemClickListener != null)
            itemClickListener.onAction(mParent, mItem, ItemContainer.Action.Like);
    }

    protected void onShare(){
        if (itemClickListener != null)
            itemClickListener.onAction(mParent, mItem, ItemContainer.Action.Share);
    }

    protected void onReply(){
        if (itemClickListener != null)
            itemClickListener.onAction(mParent, mItem, ItemContainer.Action.Reply);
    }

    public abstract void setActionVisibility(ItemContainer.Action action, boolean visible);

    public void hide() {
        Animation anim = new AlphaAnimation(1, 0);
        anim.setDuration(300);
        startAnimation(anim);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ((ViewGroup)getParent()).removeView(ItemContextMenuView.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
