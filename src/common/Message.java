package common;

import java.io.Serializable;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class Message implements Serializable {
    private String sender;
    private String receiver;
    private String content;
    private String sendTime;
    private MessageType messageType;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
