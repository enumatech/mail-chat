package io.enuma.app.keystoretest;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.util.SortedList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("io.enuma.app.keystoretest", appContext.getPackageName());

        Keychain.deleteAllKeys();

        assertEquals(Keychain.findOrGenerateKeyPair("c", appContext), Keychain.findOrGenerateKeyPair("c", appContext));
        assertEquals(Keychain.findOrGenerateKey(appContext), Keychain.findOrGenerateKey(appContext));

        assertEquals(Keychain.decryptString(Keychain.encryptString("bla")), "bla");

        final byte[] bla = "bla".getBytes();

        PrivateKey pk = Keychain.getPrivateKey("c");
        PublicKey pub = Keychain.getPublicKey("c");
        byte[] blaout = Keychain.decryptRSA(pk, Keychain.encryptRSA(pub, bla));
        assertArrayEquals(blaout, bla);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            assertEquals(Keychain.findOrGenerateSecretKey("a"), Keychain.findOrGenerateSecretKey("a"));

            SecretKey key = Keychain.getSecretKey("a");
            blaout = Keychain.decryptAES(key, Keychain.encryptAES(key, bla));
            assertArrayEquals(blaout, bla);
        }

        DbOpenHelper db = new DbOpenHelper(appContext);
        ChatContact testContact = new ChatContact("test@example.com");
        testContact.lastMessageDate = new Date();
        testContact.lastMessage = "lastMessage";
        testContact.name = "name";
        testContact.pubkeyhash = "asdf";
        testContact.avatarEtag = "etag";
        testContact.avatar = Bitmap.createBitmap(16, 16, Bitmap.Config.RGB_565);
        try {
            db.removeContact(testContact);
            db.addContact(testContact);
            Collection<ChatContact> all = db.readAllContacts();

            DbOpenHelper db2 = new DbOpenHelper(appContext);
            ChatContact[] afteradd = db2.readAllContacts().toArray(new ChatContact[0]);

            assertEquals(afteradd.length, all.size());

            int i = 0;//Arrays.afteradd.indexOf(testContact);
            assertEquals(0, i);
            assertEquals(afteradd[0].email, testContact.email);
            assertEquals(afteradd[0].name, testContact.name);
            assertEquals(afteradd[0].pubkeyhash, testContact.pubkeyhash);

            for (int t=0; t<all.size(); ++t) {
//                assert afteradd.remove(all.get(t));
            }
            assertEquals(0, afteradd.length);

            db.updateContact(testContact);

            db2 = new DbOpenHelper(appContext);
            afteradd = db2.readAllContacts().toArray(new ChatContact[0]);
            int index = 0;//afteradd.indexOf(testContact);
            assertEquals(testContact.lastMessage, afteradd[index].lastMessage);
            assertEquals(testContact.avatarEtag, afteradd[index].avatarEtag);
            assertEquals(testContact.lastMessageDate, afteradd[index].lastMessageDate);
            assertEquals(testContact.avatar.getHeight(), afteradd[index].avatar.getHeight());
        }
        finally {
            db.removeContact(testContact);
        }

        DbOpenHelper db2 = new DbOpenHelper(appContext);
        ChatContact[] afterRemove = db2.readAllContacts().toArray(new ChatContact[0]);
        //assertEquals(afterRemove.indexOf(testContact), -1);
    }
}
