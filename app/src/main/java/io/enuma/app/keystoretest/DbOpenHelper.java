package io.enuma.app.keystoretest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
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

    private static final int DATABASE_VERSION = 2;
    private static final String DICTIONARY_TABLE_NAME = "contacts";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + DICTIONARY_TABLE_NAME + " (" +
                    "email TEXT NOT NULL PRIMARY KEY COLLATE NOCASE, " +
                    "name TEXT, " +
                    "pubkeyhash TEXT KEY, " +
                    "lastMessage TEXT );";

    DbOpenHelper(Context context) {
        super(context, "mailchatdb", null, DATABASE_VERSION);
    }

    public static ChatContact getContact(String sender) {
        return ITEM_MAP.get(sender);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //nop
    }

    public void readAll() {
        Cursor cursor = getReadableDatabase().query(DICTIONARY_TABLE_NAME, new String[] {"email","name","pubkeyhash"}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            ChatContact chatContact = new ChatContact(cursor.getString(0));
            chatContact.name = cursor.getString(1);
            chatContact.pubkeyhash = cursor.getString(2);
            addItem(chatContact);
        }
    }

    private void addItem(ChatContact item) {
        ChatContact previous = ITEM_MAP.put(item.id(), item);
        //if (previous)
        ITEMS.add(item);
    }

    public void addContact(ChatContact item) {
        addItem(item);

        ContentValues contentValues = new ContentValues();
        contentValues.put("email", item.email);
        contentValues.put("name", item.name);
        contentValues.put("pubkeyhash", item.pubkeyhash);
        getWritableDatabase().insert(DICTIONARY_TABLE_NAME, null, contentValues);
    }

    public static List<ChatContact> getContacts() {
        return ITEMS;
    }
}
