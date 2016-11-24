package io.enuma.app.keystoretest;

/**
 * Created by llunesu on 13/11/2016.
 */

public class ChatMessage {

    enum Status {
        New,
        Delivered,
        Failed
    }

    ChatMessage(String message, String messageId, String senderName) {
        this.message = message;
        this.messageId = messageId;
        this.senderName = senderName;
    }

    public static ChatMessage createMine(String message, String messageId) {
        return new ChatMessage(message, messageId, null);
    }

    public static ChatMessage createOthers(String message, String messageId, String senderName) {
        return new ChatMessage(message, messageId, senderName);
    }

    public static ChatMessage createSystem(String message) {
        return new ChatMessage(message, null, null);
    }

    public String id() { return messageId; }

    public boolean isOthers() { return senderName != null && messageId != null; }
    public boolean isMine() { return senderName == null && messageId != null; }
    public boolean isSystem() { return senderName == null && messageId == null; }

    //public String subject;
    public String message;
    public String messageId;//null = datetime
    public String senderName;//null = me
    public Status status = Status.New;
    public int position = 0;
}
