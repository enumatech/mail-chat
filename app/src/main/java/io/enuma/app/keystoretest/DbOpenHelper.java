package io.enuma.app.keystoretest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.enuma.app.keystoretest.ChatContact;
import io.enuma.app.keystoretest.ChatMessage;

public class DbOpenHelper extends SQLiteOpenHelper {

    /**
     * An array of sample (dummy) items.
     */
    private static final List<ChatContact> ITEMS = new ArrayList<ChatContact>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    private static final Map<String, ChatContact> ITEM_MAP = new HashMap<String, ChatContact>();

    private static final int DATABASE_VERSION = 5;

    private static final String CONTACTS_TABLE_NAME = "contacts";
    private static final String CONTACTS_TABLE_CREATE =
            "CREATE TABLE " + CONTACTS_TABLE_NAME + " (" +
                    "email TEXT NOT NULL PRIMARY KEY COLLATE NOCASE, " +
                    "name TEXT, " +
                    "pubkeyhash TEXT, " +
                    "lastMessage TEXT, " +
                    "hidden BOOLEAN NOT NULL DEFAULT 0 );";

    private static final String MESSAGES_TABLE_NAME = "messages";
    private static final String MESSAGES_TABLE_CREATE =
            "CREATE TABLE " + MESSAGES_TABLE_NAME + " (" +
                    "thread TEXT NOT NULL, " +
                    "senderName TEXT, " +
                    "message TEXT NOT NULL, " +
                    "timestamp DATE NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "messageid TEXT NULL );";
    private static final String MESSAGES_INDEX_CREATE =
            "CREATE UNIQUE INDEX IDX_messages_messageid ON " + MESSAGES_TABLE_NAME + "(messageid);";
    private static final String MESSAGES_INDEX_CREATE2 =
            "CREATE INDEX IDX_messages_thread_timestamp ON " + MESSAGES_TABLE_NAME + "(thread,timestamp);";

    DbOpenHelper(Context context) {
        super(context, "mailchatdb", null, DATABASE_VERSION);
    }

    public static ChatContact getContact(String sender) {
        return ITEM_MAP.get(sender);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CONTACTS_TABLE_CREATE);
        db.execSQL(MESSAGES_TABLE_CREATE);
        db.execSQL(MESSAGES_INDEX_CREATE);
        db.execSQL(MESSAGES_INDEX_CREATE2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 3) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT 0;");
        }
        if (newVersion == 5) {
            db.execSQL(MESSAGES_TABLE_CREATE);
            db.execSQL(MESSAGES_INDEX_CREATE);
            db.execSQL(MESSAGES_INDEX_CREATE2);
        }
    }

    public List<ChatMessage> readAllMessages(String thread) {
        Cursor cursor = getReadableDatabase().query(MESSAGES_TABLE_NAME,
                new String[] {"message","messageid","senderName"},
                "thread = ?", new String[]{thread}, null, null, "timestamp");
        List<ChatMessage> list = new ArrayList<ChatMessage>();
        while (cursor.moveToNext()) {
            ChatMessage chatMessage = new ChatMessage(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            list.add(chatMessage);
        }
        cursor.close();
        return list;
    }

    public void readAll() {
        Cursor cursor = getReadableDatabase().query(CONTACTS_TABLE_NAME,
                new String[] {"email","name","pubkeyhash"},
                "hidden = 0", null, null, null, null);
        while (cursor.moveToNext()) {
            ChatContact chatContact = new ChatContact(cursor.getString(0));
            chatContact.name = cursor.getString(1);
            chatContact.pubkeyhash = cursor.getString(2);
            addItem(chatContact);
        }
        cursor.close();
    }

    private int addItem(ChatContact item) {
        ChatContact previous = ITEM_MAP.put(item.id(), item);
        assert previous == null;
        return ITEMS.add(item) ? ITEMS.size() - 1 : -1;
    }

    public void removeContact(ChatContact item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("hidden", 1);
        getWritableDatabase().update(CONTACTS_TABLE_NAME, contentValues, "email = ?", new String[]{item.email});
        ITEM_MAP.remove(item.id());
        ITEMS.remove(item);
    }

    public boolean saveMessage(String thread, ChatMessage item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("thread", thread);
        contentValues.put("message", item.message);
        contentValues.put("senderName", item.senderName);
        contentValues.put("messageid", item.messageId);
        return getWritableDatabase().insert(MESSAGES_TABLE_NAME, null, contentValues) != -1;
    }

    public int addContact(ChatContact item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", item.email);
        contentValues.put("name", item.name);
        contentValues.put("pubkeyhash", item.pubkeyhash);
        getWritableDatabase().insert(CONTACTS_TABLE_NAME, null, contentValues);

        return addItem(item);
    }

    public static List<ChatContact> getContacts() {
        return ITEMS;//ITEM_MAP.values();
    }
}
