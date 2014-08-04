package ru.babay.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 18.12.12
 * Time: 15:43
 */
public abstract class LoadingListItemViewBase extends RelativeLayoutFromResource {
    Runnable mLoadRunnable;

    public LoadingListItemViewBase(Context context) {
        super(context);
        localSharedConstructor();
    }

    public LoadingListItemViewBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        localSharedConstructor();
    }

    public LoadingListItemViewBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        localSharedConstructor();
    }

    void localSharedConstructor(){
        setMinimumHeight((int) (getResources().getDisplayMetrics().density * 60));
    }

    public abstract void setLoading();

    public abstract void setErrorLoading();

    public abstract void setOfflineMode();

    View.OnClickListener doLoadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mLoadRunnable != null){
                mLoadRunnable.run();
                setLoading();
            }
        }
    };

    public void setLoadRunnable(Runnable loadRunnable) {
        this.mLoadRunnable = loadRunnable;
    }

    public abstract void onAct();
}
