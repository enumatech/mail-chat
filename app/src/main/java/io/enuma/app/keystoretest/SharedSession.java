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

    private static Session session;
    private static boolean useSSL;

    public static final synchronized Session getSession(boolean ssl) {

        if (session == null || ssl != useSSL) {
            useSSL = ssl;
            Properties props = new Properties();
            props.put("mail.store.protocol", "imap");
            if (ssl) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.imap.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.imap.starttls.required", "true");
                props.put("mail.imap.starttls.enable", "true");
            }
            props.put("mail.imap.usesocketchannels", "true");//for IdleManager
            props.put("mail.imap.connectiontimeout", "5000");
            props.put("mail.imap.timeout", "5000");

            props.put("mail.transport.protocol", "smtp");
            //props.put("mail.host", smtpServer);
            //props.put("mail.smtp.port", "587");
            //props.put("mail.smtp.socketFactory.port", "465");
            //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            //props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            //props.put("mail.smtp.quitwait", "false");

            session = Session.getInstance(props);
        }
        return session;
    }

}
