package io.enuma.app.keystoretest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import static io.enuma.app.keystoretest.ChatThreadListActivity.ADD_REQUEST_CODE;
import static io.enuma.app.keystoretest.Constants.ADD_MESSAGE;
import static io.enuma.app.keystoretest.Constants.MAILCHAT_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_ID;
import static io.enuma.app.keystoretest.Constants.MESSAGE_INREPLYTO;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_NAME;
import static io.enuma.app.keystoretest.Constants.MESSAGE_RECIPIENT_EMAIL;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_EMAIL;
import static io.enuma.app.keystoretest.Constants.MESSAGE_STATUS;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_TEXT;
import static io.enuma.app.keystoretest.Constants.UPDATE_MESSAGE_STATUS;
import static io.enuma.app.keystoretest.ShareContactActivity.ARG_DISPLAY_NAME;
import static io.enuma.app.keystoretest.ShareContactActivity.ARG_EMAIL_ADDRESS;

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

    public static final int FETCH_COUNT = 20;


    /**
     * The dummy content this fragment is presenting.
     */
    private ChatContact mItem;

    private InternetAddress[] recipients;
    private String inReplyTo;
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
        setHasOptionsMenu(true);

        if (getArguments().containsKey(ARG_ITEM_ID)) {

            mItem = DbOpenHelper.getContact(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            activity.setTitle(mItem.toString());
//            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
//            if (appBarLayout != null) {
//                appBarLayout.setTitle(mItem.toString());
//            }
        }

        //session = createSession(getContext());
    }

    public final Map<String, ChatMessage> pendingMessageMap = new HashMap<String, ChatMessage>();


    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ADD_MESSAGE) {
                String sender = intent.getStringExtra(MESSAGE_SENDER_EMAIL);
                String text = intent.getStringExtra(MESSAGE_TEXT);
                String messageId = intent.getStringExtra(MESSAGE_ID);
                String subject = intent.getStringExtra(MESSAGE_SUBJECT);
                if (messageId == null) {
                    addMessage(ChatMessage.createSystem(text));
                }
                else if (sender == null) {
                    ChatMessage chatMessage = ChatMessage.createMine(text, messageId);
                    addMessage(chatMessage);
                    pendingMessageMap.put(messageId, chatMessage);
                }
                else if (sender.equalsIgnoreCase(mItem.email)) {
                    if (!subject.equals(lastSubject)) {
                        addMessage(ChatMessage.createSystem(subject));
                        lastSubject = subject;
                    }
                    addMessage(ChatMessage.createOthers(text, messageId, sender));
                }

            } else if (intent.getAction() == UPDATE_MESSAGE_STATUS) {
                String messageId = intent.getStringExtra(MESSAGE_ID);
                int status = intent.getIntExtra(MESSAGE_STATUS, 1);
                ChatMessage chatMessage = pendingMessageMap.get(messageId);
                chatMessage.status = ChatMessage.Status.values()[status];
                ((CardListAdapter) recyclerView.getAdapter()).updateMessageStatus(chatMessage);
                new DbOpenHelper(getContext()).updateMessage(mItem.email, chatMessage);

                if (status == ChatMessage.Status.Delivered.ordinal()) {
                    pendingMessageMap.remove(messageId);
                }
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.app_bar_person:
                // Show the group's / contact's QR
                Intent intent = new Intent(getContext(), ShareContactActivity.class);
                intent.putExtra(ARG_DISPLAY_NAME, mItem.name);
                intent.putExtra(ARG_EMAIL_ADDRESS, mItem.email);
                startActivityForResult(intent, ADD_REQUEST_CODE);
                return true;
            case R.id.app_bar_settings:
                SettingsActivity.showPreferences(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Show the content as chat history
        if (mItem != null) {

            // Read old messages, if any
            if (mItem.history == null) {
                mItem.history = new DbOpenHelper(getContext()).readMessages(mItem.email, FETCH_COUNT);
            }

            // Use the message-ID of the last message for the next reply
            if (mItem.history.size() > 0) {
                inReplyTo = mItem.history.get(mItem.history.size() - 1).messageId;
            }

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getContext());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            recyclerView = (RecyclerView)rootView.findViewById(R.id.cardList);

            final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    // Load all message in thread
                    new AsyncTask<DbOpenHelper,Void,List<ChatMessage>>() {
                        @Override
                        protected List<ChatMessage> doInBackground(DbOpenHelper... params) {
                            return params[0].readMessages(mItem.email, 0);
                        }

                        @Override
                        protected void onPostExecute(List<ChatMessage> list) {
                            final CardListAdapter cardListAdapter = new CardListAdapter(list);
                            recyclerView.setAdapter(cardListAdapter);
                            cardListAdapter.notifyDataSetChanged();
                            // TODO: jump to the correct position
                        }
                    }.execute(new DbOpenHelper(getContext()));

                    swipeRefreshLayout.setRefreshing(false);
                    swipeRefreshLayout.setEnabled(false);
                }
            });

            final CardListAdapter cardListAdapter = new CardListAdapter(mItem.history);
            recyclerView.setAdapter(cardListAdapter);
            recyclerView.smoothScrollToPosition(cardListAdapter.getItemCount());

            recyclerView.setLayoutManager(linearLayoutManager);

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

            final EditText editText = (EditText)rootView.findViewById(R.id.message_et);
            editText.requestFocus();
            if (mItem.history.size() == 0) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }

            try {
                recipients = new InternetAddress[]{ mItem.getAddress() };
                //receiveMail(recipients);

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                final String emailAddress = sharedPreferences.getString("email_address", "");
                final String displayName = sharedPreferences.getString("display_name", "");
                //final Address address = new InternetAddress(emailAddress, displayName == "" ? null : displayName);

                Button sendMailButton = (Button) rootView.findViewById(R.id.send_message);
                sendMailButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text = editText.getText().toString().trim();
                        if (text.length() > 0) {
                            sendMail(text, emailAddress, displayName, recipients[0].getAddress());
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

    private void sendMail(String text, String address, String name, String recipient) {
        Intent intent = new Intent(getActivity(), SmtpService.class);
        intent.putExtra(MESSAGE_TEXT, text);
        intent.putExtra(MESSAGE_SUBJECT, lastSubject);
        intent.putExtra(MESSAGE_SENDER_EMAIL, address);
        intent.putExtra(MESSAGE_SENDER_NAME, name);
        intent.putExtra(MESSAGE_RECIPIENT_EMAIL, recipient);
        intent.putExtra(MESSAGE_INREPLYTO, inReplyTo);
        getActivity().startService(intent);
    }


    private void addMessage(ChatMessage chatMessage) {
        ((CardListAdapter)recyclerView.getAdapter()).addMessage(chatMessage);
        //recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount()-1);
        if (chatMessage.messageId != null) {
            inReplyTo = chatMessage.messageId;
        }
        mItem.lastMessage = ChatContact.summarize(chatMessage.message);
        new DbOpenHelper(getContext()).addMessage(mItem.email, chatMessage);
    }
}
