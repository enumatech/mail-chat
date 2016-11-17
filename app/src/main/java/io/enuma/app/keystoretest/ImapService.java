package io.enuma.app.keystoretest;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IdleManager;

import java.io.IOException;
import java.security.Key;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import static io.enuma.app.keystoretest.Constants.ADD_MESSAGE;
import static io.enuma.app.keystoretest.Constants.MESSAGE_ID;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_TEXT;

/**
 * Created by llunesu on 17/11/2016.
 */

public class ImapService extends Service {

    private Thread thread;

    @Override
    public void onCreate() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveMail();
                try {
                    store.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Store store;


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
    }

    //private boolean textIsHtml = false;

    /**
     * Return the primary text content of the message.
     */
    private static String getText(Part p) throws
            MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            //textIsHtml = p.isMimeType("text/html");
            return s;

        } else if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text?
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;

                } else if (bp.isMimeType("text/html")) {
                    if (text == null)
                        text = getText(bp);

                } else {
                    return getText(bp);
                }
            }
            return text;

        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }


    final static int FETCH_COUNT = 100;
    final static Pattern chatPattern = Pattern.compile("\\A\\s*(.*?)\\s*(^>>|^> |^--|\\z|^—|^__)", Pattern.DOTALL|Pattern.MULTILINE);

    private String emailAddress;

    public void receiveMail() {
        try {

            final Session session = SharedSession.getSession();

            if (store == null) {
                store = session.getStore();
            }

            ExecutorService es = Executors.newCachedThreadPool();
            final IdleManager idleManager = new IdleManager(session, es);

            if (!store.isConnected()) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String imapPassword = sharedPreferences.getString("imap_password", "");
                String imapUsername = sharedPreferences.getString("imap_username", "");
                String imapServer = sharedPreferences.getString("imap_server", "");
                emailAddress = sharedPreferences.getString("email_address", "");
                try {
                    Key secretKey = Keychain.getSecretKey(getBaseContext(), EncryptedEditTextPreference.KEY_ALIAS);
                    imapPassword = Keychain.decryptString(secretKey, imapPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                store.connect(imapServer, imapUsername, imapPassword);
            }

            final IMAPFolder folder = (IMAPFolder)store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            final FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add("Message-ID");

            int count = folder.getMessageCount();
            Log.v("imap","INBOX count "+count);

            Message[] msgs = folder.getMessages(count <= FETCH_COUNT ? 1 : count - FETCH_COUNT, count);
            folder.fetch(msgs, fp);
            parseMessages(msgs);

            folder.addMessageCountListener(new MessageCountListener() {
                @Override
                public void messagesAdded(MessageCountEvent e) {
                    try {
                        folder.fetch(e.getMessages(), fp);
                        parseMessages(e.getMessages());
                        idleManager.watch((Folder)e.getSource()); // keep watching for new messages
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        // Retry later?
                    }
                }

                @Override
                public void messagesRemoved(MessageCountEvent e) {
                    // NOP
                }
            });

                        /*
This delivers the events for each folder in a separate thread, NOT using the Executor. To deliver all events in a single thread using the Executor, set the following properties for the Session (once), and then add listeners and watch the folder as above.
        // the following should be done once...
        Properties props = session.getProperties();
        props.put("mail.event.scope", "session"); // or "application"
        props.put("mail.event.executor", es);
                         */
            idleManager.watch(folder);

            //publishProgress(ChatMessage.createSystem(e2.getLocalizedMessage()));


        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseMessages(Message[] msgs) {

        for (int i=0; i<msgs.length;i++) {
            try {
                Address[] recipientAddresses = (Address[]) msgs[i].getAllRecipients();
                if (recipientAddresses != null
                        && recipientAddresses.length == 1
                        && recipientAddresses[0] instanceof InternetAddress
                        && ((InternetAddress)recipientAddresses[0]).getAddress().equalsIgnoreCase(emailAddress)) {

                    Address sender = msgs[i].getFrom()[0];
                    Log.v("imap", "Got mail from "+sender+", for "+recipientAddresses[0]);
                    String[] messageID = msgs[i].getHeader("Message-ID");
                    String senderAddress = ((InternetAddress) sender).getAddress();

                    String message = getText(msgs[i]);//lazily
                    if (message != null) {
                        Matcher m = chatPattern.matcher(message);
                        if (m.find() && m.group(1) != null && m.group(1).length() > 0) {

                            String subject = msgs[i].getSubject();
/*
                                        if (subject != null && !subject.equals(lastSubject)) {
                    addMessage(msgs[0], msgs[1], msgs[2]);
                                            publishProgress(ChatMessage.createSystem(subject));
                                            lastSubject = subject;
                                        }
*/

                            addMessage(messageID[0], m.group(1), senderAddress, subject);
                            continue;
                        }
                    }

                    addMessage(null, "Could not extract message content", null, null);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private void addMessage(String messageId, String text, String sender, String subject) {
        Intent intent = new Intent(ADD_MESSAGE);
        intent.putExtra(MESSAGE_TEXT, text);
        intent.putExtra(MESSAGE_ID, messageId);
        intent.putExtra(MESSAGE_SENDER, sender);
        intent.putExtra(MESSAGE_SUBJECT, subject);
        sendBroadcast(intent);
    }

}

