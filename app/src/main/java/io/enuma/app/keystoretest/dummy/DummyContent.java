package io.enuma.app.keystoretest.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.enuma.app.keystoretest.ChatContact;
import io.enuma.app.keystoretest.ChatMessage;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<ChatContact> ITEMS = new ArrayList<ChatContact>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, ChatContact> ITEM_MAP = new HashMap<String, ChatContact>();

    public static void addItem(ChatContact item) {
        ChatContact previous = ITEM_MAP.put(item.id(), item);
        //if (previous)
        ITEMS.add(item);
    }

}
