package io.enuma.app.keystoretest;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.BoolRes;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IdleManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import io.enuma.app.keystoretest.dummy.DummyContent;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static io.enuma.app.keystoretest.ChatThreadListActivity.ADD_REQUEST_CODE;
import static io.enuma.app.keystoretest.ShareContactActivity.ARG_DISPLAY_NAME;
import static io.enuma.app.keystoretest.ShareContactActivity.ARG_EMAIL_ADDRESS;
import static java.util.regex.Pattern.DOTALL;

/**
 * A fragment representing a single ChatThread detail screen.
 * This fragment is either contained in a {@link ChatThreadListActivity}
 * in two-pane mode (on tablets) or a {@link ChatThreadDetailActivity}
 * on handsets.
 */
public class ChatThreadDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    public static final String MAILCHAT_SUBJECT = "sent by mailchat";

    /**
     * The dummy content this fragment is presenting.
     */
    private ChatContact mItem;

    private InternetAddress[] recipients;
    private String inReplyTo;
    private String emailAddress;
    private RecyclerView recyclerView;
    private volatile String lastSubject = MAILCHAT_SUBJECT;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChatThreadDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            activity.setTitle(mItem.toString());
//            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
//            if (appBarLayout != null) {
//                appBarLayout.setTitle(mItem.toString());
//            }
        }

        session = createSession(getContext());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Show the content as chat history
        if (mItem != null) {

            FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Show the group's / contact's QR
                    Intent intent = new Intent(view.getContext(), ShareContactActivity.class);
                    intent.putExtra(ARG_DISPLAY_NAME, mItem.name);
                    intent.putExtra(ARG_EMAIL_ADDRESS, mItem.email);
                    startActivityForResult(intent, ADD_REQUEST_CODE);
                }
            });

            if (mItem.history == null) {
                mItem.history = new ArrayList<ChatMessage>();
            }
            CardListAdapter cardListAdapter = new CardListAdapter(mItem.history);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getContext());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            recyclerView = (RecyclerView)rootView.findViewById(R.id.cardList);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(cardListAdapter);
            recyclerView.smoothScrollToPosition(cardListAdapter.getItemCount());
            //cardListAdapter.notifyDataSetChanged();

            // Scroll down if the keyboard is opened or closed
            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v,
                                           int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bottom < oldBottom) {
                        recyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
                            }
                        }, 100);
                    }
                }
            });

            try {
                recipients = new InternetAddress[]{ mItem.getAddress() };
                receiveMail(recipients);

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                emailAddress = sharedPreferences.getString("email_address", "");
                String displayName = sharedPreferences.getString("display_name", "");
                final Address address = new InternetAddress(emailAddress, displayName == "" ? null : displayName);

                Button sendMailButton = (Button) rootView.findViewById(R.id.send_message);
                sendMailButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText editText = (EditText)rootView.findViewById(R.id.message_et);
                        String text = editText.getText().toString().trim();
                        if (text.length() > 0) {
                            sendMail(lastSubject, text, address, recipients);
                        }
                        editText.getText().clear();
                    }
                });

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                addMessage(ChatMessage.createSystem(e.getLocalizedMessage()));
            }
        }

        return rootView;
    }


    static Session createSession(final Context context) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.starttls.enable","true");
        props.put("mail.imap.starttls.required","true");
        props.put("mail.imap.usesocketchannels", "true");//for IdleManager

        props.put("mail.transport.protocol", "smtp");
        //props.put("mail.host", smtpServer);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.starttls.required","true");
        //props.put("mail.smtp.socketFactory.port", "465");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.auth", "true");
        //props.put("mail.smtp.quitwait", "false");

//        AccountManager accountManager = AccountManager.get(null);
//        accountManager.addAccount();
//        accountManager.getAccountsByType("smtp");

        return Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                String smtpPassword = sharedPreferences.getString("smtp_password", "");
                String smtpUsername = sharedPreferences.getString("smtp_username", "");
                try {
                    Key secretKey = Keychain.getSecretKey(context, EncryptedEditTextPreference.KEY_ALIAS);
                    smtpPassword = new String(Keychain.decrypt(secretKey, Base64.decode(smtpPassword, 0)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
    }

    private Session session;
    private Transport transport;
    private Store store;


    private boolean textIsHtml = false;

    /**
     * Return the primary text content of the message.
     */
    private static String getText(Part p) throws
            MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            //textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
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


    final static Pattern chatPattern = Pattern.compile("\\A\\s*(.*?)\\s*(^>>|^> |^--|\\z|—)", Pattern.DOTALL|Pattern.MULTILINE);


    public void receiveMail(final InternetAddress[] recipients) {
        try {

            if (store == null) {
                store = session.getStore();
            }

            ExecutorService es = Executors.newCachedThreadPool();
            final IdleManager idleManager = new IdleManager(session, es);

            new AsyncTask<Boolean, ChatMessage, Void>() {
                @Override
                protected Void doInBackground(Boolean... parseFirstN) {
                    try {
                        if (!store.isConnected()) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                            String imapPassword = sharedPreferences.getString("imap_password", "");
                            String imapUsername = sharedPreferences.getString("imap_username", "");
                            String imapServer = sharedPreferences.getString("imap_server", "");
                            try {
                                Key secretKey = Keychain.getSecretKey(getContext(), EncryptedEditTextPreference.KEY_ALIAS);
                                imapPassword = new String(Keychain.decrypt(secretKey, Base64.decode(imapPassword, 0)));
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
                        if (parseFirstN[0]) {
                            Message[] msgs = folder.getMessages(count <= 100 ? 1 : count - 100, count);
                            folder.fetch(msgs, fp);
                            parseMessages(msgs);
                        }

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


                    } catch (Exception e2) {
                        publishProgress(ChatMessage.createSystem(e2.getLocalizedMessage()));
                        e2.printStackTrace();
                    }
                    return null;
                }

                private void parseMessages(Message[] msgs) {

                    for (int i=0; i<msgs.length;i++) {
                        try {
                            String[] messageID = msgs[i].getHeader("Message-ID");
                            Address sender = msgs[i].getFrom()[0];
                            Address[] recipientAddresses = (Address[]) msgs[i].getAllRecipients();
                            Log.v("imap", "Got mail from "+sender+", for "+recipientAddresses[0]);
                            String senderAddress = ((InternetAddress) sender).getAddress();

                            if (recipients[0].getAddress().equalsIgnoreCase(senderAddress)
                                    && recipientAddresses.length == 1
                                    && recipientAddresses[0] instanceof InternetAddress
                                    && ((InternetAddress)recipientAddresses[0]).getAddress().equalsIgnoreCase(emailAddress)) {

                                String message = getText(msgs[i]);
                                if (message != null) {
                                    Matcher m = chatPattern.matcher(message);
                                    m.find();
                                    if (m.group(1) != null && m.group(1).length() > 0) {

                                        String subject = msgs[i].getSubject();
                                        if (subject != null && !subject.equals(lastSubject)) {
                                            publishProgress(ChatMessage.createSystem(subject));
                                            lastSubject = subject;
                                        }

                                        publishProgress(ChatMessage.createOthers(m.group(1), messageID[0], senderAddress));
                                        continue;
                                    }
                                }

                                publishProgress(ChatMessage.createSystem("Could not extract message content"));
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }

                @Override
                protected void onProgressUpdate(ChatMessage... msgs) {
                    addMessage(msgs[0]);
                    if (msgs[0].messageId != null) {
                        inReplyTo = msgs[0].messageId;
                    }
                }

            }.execute(mItem.history.size() == 0);

        }
        catch(javax.mail.NoSuchProviderException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized String sendMessage(Message message) {
        try {
            if (!transport.isConnected()) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                final String smtpServer = sharedPreferences.getString("smtp_server", "unconfigured");
                transport.connect(smtpServer, null, null);
            }
            transport.sendMessage(message, message.getAllRecipients());
            return null;
        } catch (MessagingException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    public void sendMail(String subject, final String body, Address sender, InternetAddress[] recipients) {
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

            final ChatMessage chatMessage = ChatMessage.createMine(body, "ID done in thread");

            new AsyncTask<MimeMessage, MimeMessage, String>() {
                @Override
                protected String doInBackground(MimeMessage... params) {
                    try {
                        params[0].saveChanges();
                        publishProgress(params);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    return sendMessage(params[0]);
                }
                @Override
                protected void onProgressUpdate(MimeMessage... params) {
                    try {
                        chatMessage.messageId = params[0].getMessageID();
                        addMessage(chatMessage);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                protected void onPostExecute(String error) {
                    chatMessage.status = (error == null) ? ChatMessage.Status.Delivered : ChatMessage.Status.Failed;
                    ((CardListAdapter)recyclerView.getAdapter()).updateMessageStatus(chatMessage);
                    inReplyTo = chatMessage.messageId;
                    if (error != null) {
                        addMessage(ChatMessage.createSystem(error));
                    }
                }
            }.execute(message);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void addMessage(ChatMessage chatMessage) {
        ((CardListAdapter)recyclerView.getAdapter()).addMessage(chatMessage);
        recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
    }
}
