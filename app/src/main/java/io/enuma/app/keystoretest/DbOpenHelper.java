package io.enuma.app.keystoretest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.util.SortedList;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import io.enuma.app.keystoretest.ChatContact;
import io.enuma.app.keystoretest.ChatMessage;

public class DbOpenHelper extends SQLiteOpenHelper {

    /**
     * A map of sample (dummy) items, by ID.
     */
    private static final Map<String, ChatContact> ITEM_MAP = new HashMap<String, ChatContact>();

    private static final int DATABASE_VERSION = 8;

    private static final String CONTACTS_TABLE_NAME = "contacts";
    private static final String CONTACTS_TABLE_CREATE =
            "CREATE TABLE " + CONTACTS_TABLE_NAME + " (" +
                    "email TEXT NOT NULL PRIMARY KEY COLLATE NOCASE, " +
                    "name TEXT, " +
                    "pubkeyhash TEXT, " +
                    "lastMessage TEXT, " +
                    "hidden BOOLEAN NOT NULL DEFAULT 0, " +
                    "timestamp DATE NOT NULL DEFAULT CURRENT_TIMESTAMP, " + //"YYYY-MM-DD HH:MM:SS" in UTC
                    "avatar BLOB, " +
                    "avatarDate TEXT );";
    private static final String CONTACTS_INDEX_CREATE =
            "CREATE INDEX IDX_contacts_timestamp ON " + CONTACTS_TABLE_NAME + "(timestamp);";

    private static final String MESSAGES_TABLE_NAME = "messages";
    private static final String MESSAGES_TABLE_CREATE =
            "CREATE TABLE " + MESSAGES_TABLE_NAME + " (" +
                    "thread TEXT NOT NULL, " +
                    "senderName TEXT, " +
                    "message TEXT NOT NULL, " +
                    "timestamp DATE NOT NULL DEFAULT CURRENT_TIMESTAMP, " + //"YYYY-MM-DD HH:MM:SS" in UTC
                    "messageid TEXT NULL," +
                    "status INTEGER DEFAULT 0 );";
    private static final String MESSAGES_INDEX_CREATE =
            "CREATE UNIQUE INDEX IDX_messages_messageid ON " + MESSAGES_TABLE_NAME + "(messageid);";
    private static final String MESSAGES_INDEX_CREATE2 =
            "CREATE INDEX IDX_messages_thread_timestamp ON " + MESSAGES_TABLE_NAME + "(thread,timestamp);";

    private static final String SQLITE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";


    private static String currentTimestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SQLITE_TIMESTAMP_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(new Date());
    }


    DbOpenHelper(Context context) {
        super(context, "mailchatdb", null, DATABASE_VERSION);
    }


    public static ChatContact getContact(String sender) {
        return ITEM_MAP.get(sender);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CONTACTS_TABLE_CREATE);
        db.execSQL(CONTACTS_INDEX_CREATE);

        db.execSQL(MESSAGES_TABLE_CREATE);
        db.execSQL(MESSAGES_INDEX_CREATE);
        db.execSQL(MESSAGES_INDEX_CREATE2);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT 0;");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN avatar BLOB;");
            db.execSQL(MESSAGES_TABLE_CREATE);
            db.execSQL(MESSAGES_INDEX_CREATE);
            db.execSQL(MESSAGES_INDEX_CREATE2);
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN avatarDate TEXT;");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN timestamp DATE NOT NULL DEFAULT 0;");
            db.execSQL(CONTACTS_INDEX_CREATE);
        }
        if (oldVersion < 8) {
            db.execSQL("UPDATE contacts SET timestamp = CURRENT_TIMESTAMP WHERE timestamp = 0;");
        }
    }


    public List<ChatMessage> readMessages(String thread, int limit) {
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor cursor = db.query(MESSAGES_TABLE_NAME,
                    new String[]{"message", "messageid", "senderName", "status"},
                    "thread = ?", new String[]{thread}, null, null, "timestamp desc", limit > 0 ? String.valueOf(limit) : null);
            ArrayList<ChatMessage> list = new ArrayList<ChatMessage>(cursor.getCount());
            while (cursor.moveToNext()) {
                ChatMessage chatMessage = new ChatMessage(cursor.getString(0), cursor.getString(1), cursor.getString(2));
                chatMessage.status = ChatMessage.Status.values()[cursor.getInt(3)];
                list.add(chatMessage);
            }
            cursor.close();
            Collections.reverse(list);
            return list;
        }
        finally {
            db.close();
        }
    }


    public Collection<ChatContact> readAllContacts() {

        assert ITEM_MAP.size() == 0;
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor cursor = db.query(CONTACTS_TABLE_NAME,
                    new String[]{"email", "name", "pubkeyhash", "lastMessage", "avatar", "avatarDate", "timestamp"},
                    "hidden = 0", null, null, null, "timestamp desc");
            while (cursor.moveToNext()) {
                ChatContact chatContact = new ChatContact(cursor.getString(0));
                chatContact.name = cursor.getString(1);
                chatContact.pubkeyhash = cursor.getString(2);
                chatContact.lastMessage = cursor.getString(3);
                byte[] blob = cursor.getBlob(4);
                if (blob != null && blob.length > 0) {
                    chatContact.avatar = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                    chatContact.avatarEtag = cursor.getString(5);
                }
                String timestamp = cursor.getString(6);
                try {
                    chatContact.lastMessageDate = new SimpleDateFormat(SQLITE_TIMESTAMP_FORMAT).parse(timestamp);
                } catch (ParseException e) {
                    chatContact.lastMessageDate = new Date();
                }
                addItem(chatContact);
            }
            cursor.close();
            return ITEM_MAP.values();
        }
        finally {
            db.close();
        }
    }


    private boolean addItem(ChatContact item) {
        ChatContact previous = ITEM_MAP.put(item.id(), item);
        return true;
        //return ITEMS.add(item);// ? ITEMS.size() - 1 : -1;
    }


    public boolean removeContact(ChatContact item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("hidden", 1);
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.update(CONTACTS_TABLE_NAME, contentValues, "email = ?", new String[]{item.email});
        }
        finally {
            db.close();
        }
        return null != ITEM_MAP.remove(item.id());
        //return ITEMS.remove(item);
    }


    public boolean updateMessage(String thread, ChatMessage item) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("status", item.status.ordinal());
            return 1 ==  db.update(MESSAGES_TABLE_NAME, contentValues, "messageid = ?", new String[]{item.messageId});
        }
        finally {
            db.close();
        }
    }


    public boolean addMessage(String thread, ChatMessage item) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("thread", thread);
            contentValues.put("message", item.message);
            contentValues.put("senderName", item.senderName);
            contentValues.put("messageid", item.messageId);
            contentValues.put("status", item.status.ordinal());
            if (0 < db.insertWithOnConflict(MESSAGES_TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE)) {
                String lastMessage = ChatContact.summarize(item.message);
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("lastMessage", lastMessage);
                contentValues2.put("timestamp", currentTimestamp());
                db.update(CONTACTS_TABLE_NAME, contentValues2, "email = ?", new String[]{thread});
                //db.execSQL("UPDATE "+CONTACTS_TABLE_NAME+" SET lastMessage=? , timestamp=CURRENT_TIMESTAMP WHERE email=?",
                //    new Object[]{ lastMessage, thread });
                return true;
            }
            else {
                return false;
            }
        }
        finally {
            db.close();
        }
    }


    public boolean updateContact(ChatContact item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", item.name);
        contentValues.put("pubkeyhash", item.pubkeyhash);
        contentValues.put("hidden", 0);
        contentValues.put("lastMessage", item.lastMessage);
        contentValues.put("timestamp", currentTimestamp());
        if (item.avatar != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (item.avatar.compress(Bitmap.CompressFormat.PNG, 1, byteArrayOutputStream)) {
                contentValues.put("avatar", byteArrayOutputStream.toByteArray());
                contentValues.put("avatarDate", item.avatarEtag);
            }
        }
        SQLiteDatabase db = getWritableDatabase();
        try {
            return 1 == db.update(CONTACTS_TABLE_NAME, contentValues, "email = ?", new String[]{item.email});
        }
        finally {
            db.close();
        }
    }


    public boolean addContact(ChatContact item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", item.email);
        contentValues.put("name", item.name);
        contentValues.put("pubkeyhash", item.pubkeyhash);
        contentValues.put("hidden", 0);
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.insertWithOnConflict(CONTACTS_TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }
        finally {
            db.close();
        }

        return addItem(item);
    }


    public static Collection<ChatContact> getContacts() {
        return ITEM_MAP.values();
    }
}
