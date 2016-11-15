package io.enuma.app.keystoretest;

import android.graphics.Bitmap;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

/**
 * Created by llunesu on 13/11/2016.
 */

public class ChatContact {
    public String email;
    public String name;
    //public String publicKey;
    //public String deviceToken;
    //public String address;
    public String last;
    public Date date;
    public Bitmap avatar;

    public List<ChatMessage> history;

    public static String summarize(String text) {
        String result = text.trim();
        if (result.length() > 31) {
            result = result.substring(0, 30) + "…";
        }
        return result.replace('\n',' ').replace('\t',' ');
    }

    public String getLastMessage() { return (history != null && history.size() > 0) ? summarize(history.get(history.size()-1).message) : ""; }

    @Override
    public String toString() {
        return String.format("%s <%s>", name, email);
    }

}

