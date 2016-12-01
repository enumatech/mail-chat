package io.enuma.app.keystoretest;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by llunesu on 1/12/2016.
 */

public class MyEditTextPreference extends EditTextPreference {

    public MyEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MyEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyEditTextPreference(Context context) {
        super(context);
    }

    public void show() {
        showDialog(null);
    }
}
