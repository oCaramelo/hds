package tecnico.communication;

import java.io.Serializable;
import tecnico.encryption.MessageSerializer;

public class Message implements Serializable {

    private String senderId;
    private int messageId = -1;
    protected Type type;
    private String signature;
    private int epoch;
    private byte[] payload;

    public Message() {}

    public Message(String senderId, Type type) {
        this.senderId = senderId;
        this.type = type;
    }

    public Message(String senderId, Type type, int epoch) {
        this.senderId = senderId;
        this.type = type;
        this.epoch = epoch;
    }

    // Getters and Setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public int getEpoch() { return epoch; }
    public void setEpoch(int epoch) { this.epoch = epoch; }
    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }

    public enum Type {
        // NODES ACTIONS
        READ, STATE, WRITE, ACCEPT,
        
        // LEDGER ACTIONS
        TRANSFER, TRANSFER_RESULT,
        SHOW_PROFILE, SHOW_PROFILE_RESULT,
        SHOW_BLOCKCHAIN, SHOW_BLOCKCHAIN_RESULT,
        SHOW_NETWORK, SHOW_NETWORK_RESULT,
        
        ERROR,
        ACK,
        IGNORE
    }

    @Override
    public String toString() {
        return "Message{" +
                "senderId='" + senderId + '\'' +
                ", type=" + type +
                ", epoch=" + epoch +
                '}';
    }

    public Message cloneMessage() {
        return MessageSerializer.cloneObject(this, (Class<Message>) this.getClass());
    }
}