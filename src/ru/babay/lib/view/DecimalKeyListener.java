package ru.babay.lib.view;

import android.text.InputType;
import android.text.method.NumberKeyListener;

/**
 * Created by IntelliJ IDEA.
 * User: babay
 * Date: 26.12.11
 * Time: 14:10
 */
public class DecimalKeyListener extends NumberKeyListener{

    @Override
    protected char[] getAcceptedChars() {
        char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ','};
        return chars;
    }

    public int getInputType() {
        return InputType.TYPE_CLASS_NUMBER;
    }
}
