package io.enuma.app.keystoretest;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.security.Key;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

/**
 * Created by llunesu on 17/11/2016.
 */

public final class SharedSession {

    public static final Session getSession() {
        return Session.getDefaultInstance(props, null);
    }

    private final static Properties props = new Properties();

    static {
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.starttls.enable", "true");
        props.put("mail.imap.starttls.required", "true");
        props.put("mail.imap.usesocketchannels", "true");//for IdleManager

        props.put("mail.transport.protocol", "smtp");
        //props.put("mail.host", smtpServer);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        //props.put("mail.smtp.socketFactory.port", "465");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.auth", "true");
        //props.put("mail.smtp.quitwait", "false");

//        AccountManager accountManager = AccountManager.get(null);
//        accountManager.addAccount();
//        accountManager.getAccountsByType("smtp");

/*
        new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                String smtpPassword = sharedPreferences.getString("smtp_password", "");
                String smtpUsername = sharedPreferences.getString("smtp_username", "");
                try {
                    Key secretKey = Keychain.getSecretKey(context, EncryptedEditTextPreference.KEY_ALIAS);
                    smtpPassword = Keychain.decryptString(secretKey, smtpPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
*/
    }

}
