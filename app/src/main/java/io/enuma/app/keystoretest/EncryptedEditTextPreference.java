package io.enuma.app.keystoretest;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Base64;


import java.security.Key;
import java.security.PublicKey;

import static android.util.Base64.NO_WRAP;

/**
 * Created by llunesu on 16/11/2016.
 */

public class EncryptedEditTextPreference extends EditTextPreference {
    public EncryptedEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public EncryptedEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EncryptedEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EncryptedEditTextPreference(Context context) {
        super(context);
    }


    @Override
    protected boolean persistString(String value) {
        try {

            String encrypted = Keychain.encryptString(value);
            return super.persistString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    protected String getPersistedString(String defaultReturnValue) {
        try {
            String base64 = super.getPersistedString("");
            if (base64.length() > 0) {
                return Keychain.decryptString(base64);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultReturnValue;
    }


    public void show() {
        showDialog(null);
    }

}
