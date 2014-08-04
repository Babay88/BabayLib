package ru.babay.lib.view;

import android.view.View;
import android.widget.ScrollView;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 05.01.13
 * Time: 13:06
 * To change this template use File | Settings | File Templates.
 */
public interface VerticalSwypeListener {
    public void onSwypeBy(ScrollView v, float dy);
    public void onSwypeRelease(ScrollView scrollView, float velocity);
    public void onSwypeStart(ScrollView v);
}
