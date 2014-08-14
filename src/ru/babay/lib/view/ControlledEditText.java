package ru.babay.lib.view;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by Babay on 05.02.14.
 */
public class ControlledEditText extends EditText {

    private static final int ID_PASTE = android.R.id.paste;

    String denyChars;

    public ControlledEditText(Context context) {
        super(context);
        setEditableFactory(ControlledEditableFactory.getInstance());
    }

    public ControlledEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEditableFactory(ControlledEditableFactory.getInstance());
    }

    public ControlledEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setEditableFactory(ControlledEditableFactory.getInstance());
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (Build.VERSION.SDK_INT >= 11)
            if (id == ID_PASTE) {
                CharSequence text = getText();
                if (text instanceof ControlledSSB){
                    ((ControlledSSB) text).stripSpans = true;
                    boolean result = super.onTextContextMenuItem(id);
                    ((ControlledSSB) text).stripSpans = false;
                    return result;
                }

            }
        return super.onTextContextMenuItem(id);
    }

    static CharSequence stripSpans(CharSequence source){
        if (source instanceof Spannable){
            SpannableStringBuilder builder = new SpannableStringBuilder(source);
            ImageSpan[] imageSpans = builder.getSpans(0, builder.length(), ImageSpan.class);
            for (int j=0; j<imageSpans.length; j++){
                int start = builder.getSpanStart(imageSpans[j]);
                int end = builder.getSpanEnd(imageSpans[j]);
                builder.replace(start, end, "");
                builder.removeSpan(imageSpans[j]);
            }
            return builder.toString();

        }
        return source;
    }

    public static class ControlledEditableFactory extends Editable.Factory{
        private static ControlledEditableFactory sInstance = new ControlledEditableFactory();

        /**
         * Returns the standard Spannable Factory.
         */
        public static Editable.Factory getInstance() {
            return sInstance;
        }

        /**
         * Returns a new SpannableString from the specified CharSequence.
         * You can override this to provide a different kind of Spannable.
         */
        @Override
        public Editable newEditable(CharSequence source) {
            return new ControlledSSB(source);
        }
    }

    public static class ControlledSSB extends SpannableStringBuilder{
        boolean stripSpans;

        public ControlledSSB() {
        }

        public ControlledSSB(CharSequence text) {
            super(text);
        }

        public ControlledSSB(CharSequence text, int start, int end) {
            super(text, start, end);
        }

        @Override
        public SpannableStringBuilder replace(int start, int end, CharSequence tb) {
            if (stripSpans)
                return super.replace(start, end, stripSpans(tb));
            else
                return super.replace(start, end, tb);
        }

        @Override
        public SpannableStringBuilder insert(int where, CharSequence tb) {
            if (stripSpans)
                return super.insert(where, stripSpans(tb));
            else
                return super.insert(where, tb);
        }
    }
}