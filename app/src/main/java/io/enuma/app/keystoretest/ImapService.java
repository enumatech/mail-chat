package io.enuma.app.keystoretest;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IdleManager;
import com.sun.mail.imap.ResyncData;

import java.io.IOException;
import java.security.Key;
import java.sql.Date;
import java.util.Calendar;
import java.util.List;
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
import javax.mail.UIDFolder;
import javax.mail.event.MailEvent;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import static io.enuma.app.keystoretest.Constants.ADD_MESSAGE;
import static io.enuma.app.keystoretest.Constants.MESSAGE_ID;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_NAME;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_EMAIL;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_TEXT;

/**
 * Created by llunesu on 17/11/2016.
 */

public class ImapService extends Service {

    private final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            receiveMail();
        }
    });

    final static int FETCH_COUNT = 100;
    final static Pattern chatPattern = Pattern.compile("\\A\\s*(.*?)\\s*(?:^>>|^> |^--|\\z|^—|^__|^On [^\n]+ wrote:$)", Pattern.DOTALL|Pattern.MULTILINE);


    final FetchProfile fp = new FetchProfile();


    public ImapService() {
        fp.add(FetchProfile.Item.ENVELOPE);
    }


    @Override
    public void onCreate() {
        thread.start();
        Log.d("imap", "service started");
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


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }


    @Override
    public void onDestroy() {
        thread.interrupt();

        new AsyncTask<Folder,Void,Void>(){
            @Override
            protected Void doInBackground(Folder... params) {
                try {
                    if (params[0] != null) {
                        params[0].getMessageCount();
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(folder);

        try {
            thread.join();
        } catch (InterruptedException e) {
        }
        Log.d("imap", "service stopped");
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

    private String emailAddress;

    private long fetchMessages(IMAPFolder f, Message[] msgs) throws MessagingException {
        Log.v("imap", "FETCH count " + msgs.length);
        f.fetch(msgs, fp);
        int count = parseMessages(msgs);
        Log.v("imap", "FETCHed count " + count);
        return count > 0 ? f.getUID(msgs[count-1]) : 0;
    }

    private IMAPFolder folder;

    public void receiveMail() {
        try {

            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            emailAddress = sharedPreferences.getString("email_address", null);
            if (emailAddress == null) {
                return;
            }

            boolean ssl = sharedPreferences.getString("pref_security", "1").equals("2");
            final Session session = SharedSession.getSession(ssl);

            //final ExecutorService es = Executors.newCachedThreadPool();
            /*
            This delivers the events for each folder in a separate thread, NOT using the Executor.
            To deliver all events in a single thread using the Executor, set the following properties
            for the Session (once), and then add listeners and watch the folder as above.

            // the following should be done once...
            Properties props = session.getProperties();
            props.put("mail.event.scope", "session"); // or "application"
            props.put("mail.event.executor", es);
            */
            //final IdleManager idleManager = new IdleManager(session, es);
            final Store store = session.getStore();

            final FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(UIDFolder.FetchProfileItem.UID);
            fp.add("Message-ID");

            while (!Thread.currentThread().isInterrupted()) {

                // Reconnect to the store
                if (!store.isConnected()) {
                    Log.v("imap", "reconnect");

                    String imapPassword = sharedPreferences.getString("imap_password", null);
                    String imapUsername = sharedPreferences.getString("imap_username", null);
                    String imapServer = sharedPreferences.getString("imap_server", "unconfigured");
                    Key secretKey = Keychain.getSecretKey(getBaseContext(), EncryptedEditTextPreference.KEY_ALIAS);
                    imapPassword = Keychain.decryptString(secretKey, imapPassword);
                    int port = ssl ? 993 : 143;
                    String[] split = imapServer.split(":");
                    if (split.length == 2) {
                        imapServer = split[0];
                        port = Integer.parseInt(split[1]);
                    }
                    store.connect(imapServer, port, imapUsername, imapPassword);

                    folder = (IMAPFolder) store.getFolder("INBOX");

                    folder.addMessageCountListener(new MessageCountListener() {
                        @Override
                        public void messagesAdded(MessageCountEvent e) {
                            try {
                                IMAPFolder f = (IMAPFolder)e.getSource();
                                long last_uid = fetchMessages(f, e.getMessages());
                                if (last_uid > 0) {
                                    sharedPreferences.edit().putLong("last_uid", last_uid).commit();
                                }
                                //idleManager.watch(f);
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
                }

                // Open the folder, if not currently open
                if (!folder.isOpen()) {

                    long uid_validity = sharedPreferences.getLong("uid_validity", 0);
                    long last_uid = sharedPreferences.getLong("last_uid", 0);

                    //ResyncData resyncData = new ResyncData(uid_validity, 0, lastUID, UIDFolder.LASTUID);
                    folder.open(Folder.READ_ONLY);
                    int count = folder.getMessageCount();
                    Log.v("imap", "INBOX count " + count);

                    long uidvalidity = folder.getUIDValidity();
                    if (uid_validity != uidvalidity) {
                        sharedPreferences.edit().putLong("uid_validity", uidvalidity).commit();
                        last_uid = 0;
                    }

                    Message[] msgs = folder.getMessagesByUID(last_uid + 1, UIDFolder.LASTUID);
                    //Message[] msgs = folder.getMessages(count <= FETCH_COUNT ? 1 : count - FETCH_COUNT, count);
                    last_uid = fetchMessages(folder, msgs);
                    if (last_uid > 0) {
                        sharedPreferences.edit().putLong("last_uid", last_uid).commit();
                    }
                }

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                try {
                    folder.idle();
                    //idleManager.watch(folder);
                }
                catch (MessagingException e) {
                    //e.printStackTrace();
                    //NOP
                }
            }

            folder.close();
            //idleManager.stop();
            store.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(getBaseContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG);
            addMessage(null, "IMAP "+e.getLocalizedMessage(), null, null, null);
        }
    }

    private int parseMessages(Message[] msgs) {

        for (int i=0; i<msgs.length;i++) {
            if (Thread.currentThread().isInterrupted()) {
                return i;
            }
            try {
                Address[] recipientAddresses = msgs[i].getAllRecipients();
                if (recipientAddresses != null
                        && recipientAddresses.length == 1
                        && recipientAddresses[0] instanceof InternetAddress
                        && emailAddress.equalsIgnoreCase(((InternetAddress)recipientAddresses[0]).getAddress())) {

                    InternetAddress sender = (InternetAddress) msgs[i].getFrom()[0];
                    String senderAddress = sender.getAddress();

                    // Only fetch emails from known senders
                    if (DbOpenHelper.getContact(senderAddress) == null) {
                        continue;
                    }

                    Log.v("imap", "Got mail from "+sender+", for "+recipientAddresses[0]);

                    String message = getText(msgs[i]);//lazily
                    if (message != null) {
                        Matcher m = chatPattern.matcher(message);
                        if (m.find() && m.group(1) != null && m.group(1).length() > 0) {
                            String[] messageID = msgs[i].getHeader("Message-ID");
                            String subject = msgs[i].getSubject();
                            String senderName = sender.getPersonal();
                            addMessage(messageID[0], m.group(1), senderAddress, senderName, subject);
                            continue;
                        }
                    }

                    addMessage(null, "Could not extract message content", null, null, null);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return msgs.length;
    }

    private void addMessage(String messageId, String text, String sender, String name, String subject) {
        Intent intent = new Intent(ADD_MESSAGE);
        intent.putExtra(MESSAGE_TEXT, text);
        intent.putExtra(MESSAGE_SENDER_NAME, name);
        intent.putExtra(MESSAGE_ID, messageId);
        intent.putExtra(MESSAGE_SENDER_EMAIL, sender);
        intent.putExtra(MESSAGE_SUBJECT, subject);
        sendBroadcast(intent);
    }

}

