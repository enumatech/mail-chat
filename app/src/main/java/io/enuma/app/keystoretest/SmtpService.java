package io.enuma.app.keystoretest;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.io.FileDescriptor;
import java.security.Key;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static io.enuma.app.keystoretest.Constants.ADD_MESSAGE;
import static io.enuma.app.keystoretest.Constants.MESSAGE_ID;
import static io.enuma.app.keystoretest.Constants.MESSAGE_INREPLYTO;
import static io.enuma.app.keystoretest.Constants.MESSAGE_RECIPIENT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER;
import static io.enuma.app.keystoretest.Constants.MESSAGE_STATUS;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_TEXT;
import static io.enuma.app.keystoretest.Constants.UPDATE_MESSAGE_STATUS;

/**
 * Created by llunesu on 17/11/2016.
 */

public class SmtpService extends Service {

    private Transport transport;


//    public SmtpService() {
//        super("SMTP service");
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_NOT_STICKY;
    }

    private AsyncTask<Void, Void, Void> lastTask;

    //@Override
    protected void onHandleIntent(final Intent intent) {
        lastTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String subject = intent.getStringExtra(MESSAGE_SUBJECT);
                String body = intent.getStringExtra(MESSAGE_TEXT);
                String sender = intent.getStringExtra(MESSAGE_SENDER);
                String recipient = intent.getStringExtra(MESSAGE_RECIPIENT);
                String inReplyTo = intent.getStringExtra(MESSAGE_INREPLYTO);
                try {
                    sendMail(subject, body, new InternetAddress(sender), InternetAddress.parse(recipient), inReplyTo);
                } catch (AddressException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute((Void)null);
    }


    public void sendMail(String subject, final String body, Address sender, InternetAddress[] recipients, String inReplyTo) {
        try{
            /*
            MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
            mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
            mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
            mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
            mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
            mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
            CommandMap.setDefaultCommandMap(mc);
            */

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String ssl = sharedPreferences.getString("pref_security", "1");
            final Session session = SharedSession.getSession(ssl == "2");

            MimeMessage message = new MimeMessage(session) {
                @Override
                protected void updateMessageID() throws MessagingException {
                    // Avoid generating a new ID after we already got one
                    if (getHeader("Message-ID") == null) {
                        super.updateMessageID();
                    }
                }
            };
            //DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
            //message.setDataHandler(handler);
            //message.setSender(sender);
            message.setSubject(subject);
            message.setRecipients(Message.RecipientType.TO, recipients);
            message.setText(body);
            message.setFrom(sender);
            if (inReplyTo != null) {
                message.setHeader("In-Reply-To", inReplyTo);
            }
            if (transport == null) {
                transport = session.getTransport();
            }

            message.saveChanges();
            String messageId = message.getMessageID();

            addMessage(messageId, body, null);
            String error = sendMessageSync(message);
            ChatMessage.Status status = (error == null) ? ChatMessage.Status.Delivered : ChatMessage.Status.Failed;
            updateMessageStatus(messageId, status.ordinal());
            if (error != null) {
                addMessage(null, error, null);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void updateMessageStatus(String messageId, int status) {
        Intent intent = new Intent(UPDATE_MESSAGE_STATUS);
        intent.putExtra(MESSAGE_ID, messageId);
        intent.putExtra(MESSAGE_STATUS, status);
        sendBroadcast(intent);
    }

    private void addMessage(String messageId, String text, String sender) {
        Intent intent = new Intent(ADD_MESSAGE);
        intent.putExtra(MESSAGE_TEXT, text);
        intent.putExtra(MESSAGE_ID, messageId);
        intent.putExtra(MESSAGE_SENDER, sender);
        sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        if (lastTask != null) {
            lastTask.cancel(true);
        }
        if (transport != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        transport.close();
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(null, null);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private synchronized String sendMessageSync(Message message) {
        try {
            if (!transport.isConnected()) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String smtpServer = sharedPreferences.getString("smtp_server", "unconfigured");
                String smtpUsername = sharedPreferences.getString("smtp_username", null);
                String smtpPassword = sharedPreferences.getString("smtp_password", null);
                try {
                    Key secretKey = Keychain.getSecretKey(getBaseContext(), EncryptedEditTextPreference.KEY_ALIAS);
                    smtpPassword = Keychain.decryptString(secretKey, smtpPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int port = 25;
                String[] split = smtpServer.split(":");
                if (split.length == 2) {
                    smtpServer = split[0];
                    port = Integer.parseInt(split[1]);
                }
                transport.connect(smtpServer, port, smtpUsername, smtpPassword);
            }
            transport.sendMessage(message, message.getAllRecipients());
            return null;
        } catch (MessagingException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }


}
