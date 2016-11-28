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
    public String pubkeyhash;
    //public String deviceToken;
    //public String address;
    public String last;
    //public Date date;
    public Bitmap avatar;
    public String avatarDate;
    public boolean avatarUpdated;
    //public String myName;

    public ChatContact(String email) {
        this.email = email;
    }

    public String id() { return email; }

    public List<ChatMessage> history;

    public static String summarize(String text) {
        String result = text.trim();
        if (result.length() > 301) {
            result = result.substring(0, 300) + "…";
        }
        return result.replace('\n',' ').replace('\t',' ');
    }

    public String getLastMessage() { return (history != null && history.size() > 0) ? summarize(history.get(history.size()-1).message) : ""; }

    public InternetAddress getAddress() throws UnsupportedEncodingException { return new InternetAddress(email, name); }

    @Override
    public String toString() {
        return name == null ? email : String.format("%s <%s>", name, email);
    }

}

