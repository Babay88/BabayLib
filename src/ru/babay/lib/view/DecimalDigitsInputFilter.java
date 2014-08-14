package ru.babay.lib.view;

/**
 * Created by IntelliJ IDEA.
 * User: babay
 * Date: 20.12.11
 * Time: 7:41
 */

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Input filter that limits the number of decimal digits that are allowed to be
 * entered.
 */
public class DecimalDigitsInputFilter implements InputFilter {

    private final int decimalDigits;

    /**
     * Constructor.
     *
     * @param decimalDigits maximum decimal digits
     */
    public DecimalDigitsInputFilter(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public CharSequence filter(CharSequence source,
                               int start,
                               int end,
                               Spanned dest,
                               int dstart,
                               int dend) {

        int dotPos = getDotPosition(dest);
        if (dotPos >= 0) {
            int srcDotPos = getDotPosition(source);
            if (srcDotPos>=0)
                if (dstart <= dotPos && dotPos <= dend)
                    return oneDotSubStr(source, dotPos);
                else
                    return noDotSubStr(source, dotPos);
            // if the text is entered before the dot
            if (dend <= dotPos) {
                return null;
            }
            if (dest.length() - dotPos > decimalDigits) {
                return "";
            }
        }

        return null;
    }

    private int getDotPosition(CharSequence chars){
        return getDotPosition(chars, 0);
    }

    private int getDotPosition(CharSequence chars, int start){
        for (int i = start; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (c == '.' || c == ',')
                return i;
        }
        return -1;
    }

    private CharSequence noDotSubStr(CharSequence chars, int dotPos){
        if (dotPos == -1)
            return null;
        if (chars.length() == 1)
            return "";
        return
            chars.subSequence(0, dotPos);
    }

    private CharSequence oneDotSubStr(CharSequence chars, int dotPos){
        if (chars.length() == 1 || dotPos == -1)
            return null;
        dotPos = getDotPosition(chars, dotPos+1);
        if (dotPos == -1)
            return null;

        return chars.subSequence(0, dotPos);
    }
}