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

    private static final int COUNT = 25;

    static {
        // Add some sample items.
        for (int i = 1; i <= COUNT; i++) {
            addItem(createDummyItem(i));
        }
    }

    private static void addItem(ChatContact item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.email, item);
    }

    private static ChatContact createDummyItem(int position) {
        ChatContact dummy = new ChatContact();
        dummy.history = makeDetails(position);
        dummy.email = "lio+test"+position+"@lunesu.com";
        if (position == 1) {
            dummy.email = "lio@lunesu.com";
        }
        dummy.name = "lio李欧#" + position;
        return dummy;
    }

    private static List<ChatMessage> makeDetails(int position) {
        List<ChatMessage> list = new ArrayList<ChatMessage>();
        list.add(ChatMessage.createSystem("test1"));
        list.add(ChatMessage.createMine("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.", "id"));
        list.add(ChatMessage.createOthers("test1", "id", "blah@blah"));
        list.add(ChatMessage.createOthers("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.", "id", "blah@blah.com"));
        list.add(ChatMessage.createSystem("test1"));
        list.add(ChatMessage.createMine("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.", "me"));
        return list;
    }

}
