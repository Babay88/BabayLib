package ru.babay.lib.view;

import android.os.Parcel;
import android.text.style.URLSpan;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 10.07.13
 * Time: 11:07
 */
public class HandledUrlSpan extends URLSpan {
    private UrlHandler handler;

    public HandledUrlSpan(String url, UrlHandler handler) {
        super(url);
        this.handler = handler;
    }

    public HandledUrlSpan(Parcel src, UrlHandler handler) {
        super(src);
        this.handler = handler;
    }


    @Override
    public void onClick(View widget) {
        if (handler == null || !handler.onUrlClick(getURL(), widget))
            super.onClick(widget);
    }

    public interface UrlHandler {
        public boolean onUrlClick(String url, View widget);
    }
}
