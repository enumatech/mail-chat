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

    private ChatMessage(String message, String messageId, String sender) {
        this.message = message;
        this.messageId = messageId;
        this.sender = sender;
    }

    public static ChatMessage createMine(String message, String messageId) {
        return new ChatMessage(message, messageId, null);
    }

    public static ChatMessage createOthers(String message, String messageId, String sender) {
        return new ChatMessage(message, messageId, sender);
    }

    public static ChatMessage createSystem(String message) {
        return new ChatMessage(message, null, null);
    }

    public String id() { return messageId; }

    public boolean isOthers() { return sender != null && messageId != null; }
    public boolean isMine() { return sender == null && messageId != null; }
    public boolean isSystem() { return sender == null && messageId == null; }

    //public String subject;
    public String message;
    public String messageId;//null = datetime
    public String sender;//null = me
    public Status status = Status.New;
    public int position = 0;
}
