package ru.babay.lib.util;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: Babay
 * Date: 9/1/13
 * Time: 9:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class FontUtil {
    public static Typeface sTypeface;

    public void setTypeface(Typeface typeface){
        sTypeface = typeface;
    }

    public static void init(Context context){
        if (sTypeface == null)
            FontUtil.sTypeface = Typeface.createFromAsset(context.getAssets(), "font/pt_sans.ttc");
    }

    public static void applyFont(View view){
        if (sTypeface == null)
            return;
        if (view instanceof TextView){
            TextView textView = (TextView) view;
            Typeface old = textView.getTypeface();
            if (old == null)
                textView.setTypeface(sTypeface);
            else {
                int style = old.getStyle();
                Typeface styledTypeface = Typeface.create(sTypeface, style);
                if (!styledTypeface.equals(old))
                    textView.setTypeface(sTypeface, style );
            }
        }

        if (view instanceof ViewGroup){
            for (int i=0; i< ((ViewGroup) view).getChildCount(); i++){
                applyFont(((ViewGroup) view).getChildAt(i));
            }
        }
    }

}
