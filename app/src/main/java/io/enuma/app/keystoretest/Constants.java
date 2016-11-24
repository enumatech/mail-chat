package io.enuma.app.keystoretest;

/**
 * Created by llunesu on 17/11/2016.
 */

public final class Constants {
    public static final String MAILCHAT_SUBJECT = "sent by mailchat";

    static final public String ADD_MESSAGE = "io.enuma.msg.addmessage";
    static final public String UPDATE_MESSAGE_STATUS = "io.enuma.msg.updatemessage";

    static final public String MESSAGE_TEXT = "io.enuma.msg.addmessage.text";
    static final public String MESSAGE_ID = "io.enuma.msg.addmessage.id";
    static final public String MESSAGE_SENDER_NAME = "io.enuma.msg.addmessage.name";
    static final public String MESSAGE_SENDER_EMAIL = "io.enuma.msg.addmessage.sender";
    static final public String MESSAGE_STATUS = "io.enuma.msg.updatemessage.status";
    static final public String MESSAGE_SUBJECT = "io.enuma.msg.addmessage.subject";
    static final public String MESSAGE_INREPLYTO = "io.enuma.msg.addmessage.inreplyto";
    static final public String MESSAGE_RECIPIENT_EMAIL = "io.enuma.msg.addmessage.recipient";


    static final public int PORT_SMTP = 25;
    static final public int PORT_SMTP_SSL = 465;
    static final public int PORT_SMTP_TLS = 587;

    static final public int PORT_IMAP_TLS = 143;
    static final public int PORT_IMAP_SSL = 993;
}
