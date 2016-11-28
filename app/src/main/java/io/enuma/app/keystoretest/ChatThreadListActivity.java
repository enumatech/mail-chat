package io.enuma.app.keystoretest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.InputStream;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.List;

import static io.enuma.app.keystoretest.Constants.ADD_MESSAGE;
import static io.enuma.app.keystoretest.Constants.MAILCHAT_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_ID;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_EMAIL;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_NAME;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_TEXT;

/**
 * An activity representing a list of ChatThreads. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ChatThreadDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ChatThreadListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private RecyclerView recyclerView;

    private DbOpenHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatthread_list);

        db = new DbOpenHelper(getBaseContext());
        if (DbOpenHelper.getContacts().size() == 0) {
            db.readAllContacts();
        }

        // TODO: we might get data here if opened by notification
        Bundle blah = this.getIntent().getExtras();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab_setting = (FloatingActionButton) findViewById(R.id.fab_settings);
        fab_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.showPreferences(ChatThreadListActivity.this);
            }
        });

        FloatingActionButton fab_email = (FloatingActionButton) findViewById(R.id.fab_email);
        fab_email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent intent = new Intent(view.getContext(), ShareContactActivity.class);
                startActivityForResult(intent, CREATE_REQUEST_CODE);
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.chatthread_list);
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(db.getContacts()));

        if (findViewById(R.id.chatthread_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ADD_MESSAGE) {
                String text = intent.getStringExtra(MESSAGE_TEXT);
                String messageId = intent.getStringExtra(MESSAGE_ID);
                String sender = intent.getStringExtra(MESSAGE_SENDER_EMAIL);
                String senderName = intent.getStringExtra(MESSAGE_SENDER_NAME);
                String subject = intent.getStringExtra(MESSAGE_SUBJECT);

                ChatContact contact = db.getContact(sender);
                if (contact ==  null && subject != null && subject.contains(MAILCHAT_SUBJECT) && sender != null) {
                    contact = new ChatContact(sender);
                    contact.name = senderName;
                    db.addContact(contact);
                }
                if (contact != null) {
                    ChatMessage chatMessage = ChatMessage.createOthers(text, messageId, senderName);
                    db.saveMessage(contact.email, chatMessage);
//                    if (contact.history == null) {
//                        contact.history = new ArrayList<>();
//                    }
//                    contact.history.add(chatMessage);
                    contact.last = text;
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
                if (sender == null && messageId == null) {
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
            }
        }
    };


    static final int CREATE_REQUEST_CODE = 11;
    static final int ADD_REQUEST_CODE = 12;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == CREATE_REQUEST_CODE) {
                //Use Data to get string
                String string = data.getStringExtra("RESULT_STRING");
                if (string != null) {

                    ChatContact chatContact = new ChatContact(string);
                    //((SimpleItemRecyclerViewAdapter) recyclerView.getAdapter()).addContact(chatContact);
                    db.addContact(chatContact);
                    recyclerView.getAdapter().notifyDataSetChanged();

                    openChat(string);
                }

            } else if (requestCode == ADD_REQUEST_CODE){

                // TODO: create a group chat

            }
        }
    }

    /** Bump this int to force showing the preference/help screen on launch */
    private final static int prefVersion = 1;

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int previouslyStarted = prefs.getInt("pref_previously_launched", 0);
        if(previouslyStarted != prefVersion) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("pref_previously_launched", prefVersion);
            edit.commit();

            SettingsActivity.showPreferences(this);
        }

        Intent intent = new Intent(this, ImapService.class);
        startService(intent);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ADD_MESSAGE);
        registerReceiver(receiver, filter);
        super.onResume();
    }


    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }


    final protected static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static volatile MessageDigest md;

    static void setGravatarImage(final ImageView imageView, final ChatContact contact) {

        if (contact.avatar != null) {
            imageView.setImageBitmap( contact.avatar );
        }
        else {
            imageView.setImageBitmap( BitmapFactory.decodeResource(imageView.getResources(), R.drawable.ic_action_avatar) );
        }

        if (!contact.avatarUpdated) {

            // Don't fetch the avatar again for this contact (during this session)
            contact.avatarUpdated = true;

            new AsyncTask<String, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(String... email) {
                    try {
                        if (md == null) {
                            md = MessageDigest.getInstance("MD5");
                        }
                        byte[] thedigest = md.digest(contact.email.toLowerCase().getBytes("UTF-8"));
                        String md5 = bytesToHex(thedigest);
                        URLConnection urlConnection = new java.net.URL("http://www.gravatar.com/avatar/" + md5 + "?s=128&d=404").openConnection();
                        if (contact.avatarDate != null) {
                            // Set the caching header
                            urlConnection.setRequestProperty("If-Modified-Since", contact.avatarDate);
                        }
                        InputStream in = urlConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        contact.avatarDate = urlConnection.getHeaderField("Date");
                        return bitmap;
                    } catch (Exception e) {
//                        Log.e("Error", e.getMessage());
//                        e.printStackTrace();
                        return null;
                    }
                }

                protected void onPostExecute(Bitmap result) {
                    if (result != null) {
                        contact.avatar = result;
                        imageView.setImageBitmap(result);
                        new DbOpenHelper(imageView.getContext()).updateContact(contact);
                    }
                }
            }.execute(contact.email);
        }
    }

    protected void openChat(String id) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(ChatThreadDetailFragment.ARG_ITEM_ID, id);
            ChatThreadDetailFragment fragment = new ChatThreadDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.chatthread_detail_container, fragment)
                    .commit();
        } else {
            //Context context = getBaseContext();
            Intent intent = new Intent(this, ChatThreadDetailActivity.class);
            intent.putExtra(ChatThreadDetailFragment.ARG_ITEM_ID, id);
            startActivity(intent);
        }
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<ChatContact> mValues;

        //public void addContact(ChatContact chatContact) {
            //mValues.add(chatContact);
        //}

        public SimpleItemRecyclerViewAdapter(List<ChatContact> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chatthread_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(holder.mItem.toString());
            holder.mContentView.setText(holder.mItem.getLastMessage());

            setGravatarImage(holder.mImageView, holder.mItem);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openChat(holder.mItem.id());
                }
            });
            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    db.removeContact(holder.mItem);
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public final ImageView mImageView;
            public ChatContact mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
                mImageView = (ImageView) view.findViewById(R.id.avatar);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }


}
