package io.enuma.app.keystoretest;

import android.accounts.AccountManager;
import android.app.Activity;
import android.os.AsyncTask;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import io.enuma.app.keystoretest.dummy.DummyContent;

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

    /**
     * The dummy content this fragment is presenting.
     */
    private ChatContact mItem;

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
    }


    private CardListAdapter cardListAdapter;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Show the content as chat history
        if (mItem != null) {
            recyclerView = (RecyclerView)rootView.findViewById(R.id.cardList);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getContext());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);

            if (mItem.history == null) {
                mItem.history = new ArrayList<ChatMessage>();
            }

            cardListAdapter = new CardListAdapter(mItem.history);
            recyclerView.setAdapter(cardListAdapter);
            recyclerView.smoothScrollToPosition(cardListAdapter.getItemCount());
            cardListAdapter.notifyDataSetChanged();

            Button sendMailButton = (Button) rootView.findViewById(R.id.send_message);
            sendMailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editText = (EditText)rootView.findViewById(R.id.message_et);
                    String text = editText.getText().toString().trim();
                    if (text.length() > 0) {
                        sendMail("sent by mailchat", text, "lio@lunesu.com", mItem.email);
                    }
                    editText.getText().clear();
                }
            });

            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v,
                                           int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bottom < oldBottom) {
                        recyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.smoothScrollToPosition(
                                        recyclerView.getAdapter().getItemCount());
                            }
                        }, 100);
                    }
                }
            });
        }

        return rootView;
    }


    static Session createSession() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.starttls.enable","true");
        props.put("mail.imap.starttls.required","true");
        props.put("mail.imap.usesocketchannels", "true");

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.host", "lunesu.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.starttls.required","true");
        //props.put("mail.smtp.socketFactory.port", "465");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.auth", "true");
        //props.put("mail.smtp.quitwait", "false");

        AccountManager accountManager = AccountManager.get(null);
        accountManager.addAccount();
        accountManager.getAccountsByType("smtp");

        return Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("smtponly", "Qwer1234!");
            }
        });
    }

    private Session session = createSession();
    private Transport transport;
    private Store store;
    private IMAPFolder folder;

    public void receiveMail() {
        try {

            if (store == null) {
                store = session.getStore();
            }

            ExecutorService es = Executors.newCachedThreadPool();
            final IdleManager idleManager = new IdleManager(session, es);

            new AsyncTask<Void, ChatMessage, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        if (!store.isConnected()) {
                            store.connect("lionello","RedHerring22");
                        }
                        folder = (IMAPFolder)store.getFolder("INBOX");
                        folder.open(Folder.READ_ONLY);
                        Log.v("imap",""+folder.getMessageCount());

                        /*
This delivers the events for each folder in a separate thread, NOT using the Executor. To deliver all events in a single thread using the Executor, set the following properties for the Session (once), and then add listeners and watch the folder as above.
        // the following should be done once...
        Properties props = session.getProperties();
        props.put("mail.event.scope", "session"); // or "application"
        props.put("mail.event.executor", es);
                         */

                        idleManager.watch(folder);

                        folder.addMessageCountListener(new MessageCountListener() {
                            @Override
                            public void messagesAdded(MessageCountEvent e) {
                                Message[] msgs = e.getMessages();
                                for (int i=0; i<msgs.length;i++) {
                                    try {
                                        String subject = msgs[i].getSubject();
                                        String[] messageID = msgs[i].getHeader("Message-ID");
                                        Address[] senders = msgs[i].getFrom();
                                        publishProgress(ChatMessage.createOthers(subject, messageID[0], senders[0].toString()));
                                    } catch (MessagingException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                                try {
                                    idleManager.watch((Folder)e.getSource()); // keep watching for new messages
                                } catch (MessagingException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            @Override
                            public void messagesRemoved(MessageCountEvent e) {
                                // NOP
                            }
                        });

                        //folder.idle();

                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    return null;
                }

                protected void onProgressUpdate(ChatMessage... msgs) {
                    cardListAdapter.addMessage(msgs[0]);
                    recyclerView.smoothScrollToPosition(cardListAdapter.getItemCount());
                }
            }.execute();
        }
        catch(javax.mail.NoSuchProviderException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized void sendMessage(Message message) {
        try {
            if (!transport.isConnected()) {
                transport.connect();
            }
            transport.sendMessage(message, message.getAllRecipients());
            //folder.idle();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendMail(String subject, String body, String sender, String recipients) {
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
                    if (getHeader("Message-ID") == null) {
                        super.updateMessageID();
                    }
                }
            };
            //DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
            //message.setDataHandler(handler);
            message.setSender(new InternetAddress(sender));
            message.setSubject(subject);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setText(body);
            message.saveChanges();
            message.setFrom(sender);
            if (transport == null) {
                transport = session.getTransport();
            }

            Log.v("mailid", message.getMessageID());
            final ChatMessage chatMessage = ChatMessage.createMine(subject, message.getMessageID());
            cardListAdapter.addMessage(chatMessage);
            recyclerView.smoothScrollToPosition(cardListAdapter.getItemCount());

            new AsyncTask<Message, Void, Void>() {
                @Override
                protected Void doInBackground(Message... params) {
                    sendMessage(params[0]);
                    //publishProgress();
                    return null;
                }
                @Override
                protected void onPostExecute(Void blah) {
                    chatMessage.status = ChatMessage.Status.Delivered;
                    cardListAdapter.updateMessageStatus(chatMessage);
                }
            }.execute(message);
        }catch(Exception e){
            Log.d("mail", e.getMessage());
        }
    }

}
