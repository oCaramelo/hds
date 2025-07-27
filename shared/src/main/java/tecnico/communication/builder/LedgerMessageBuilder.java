package tecnico.communication.builder;

import tecnico.communication.LedgerMessage;
import tecnico.communication.Message;
import java.math.BigInteger;

public class LedgerMessageBuilder {

    private final LedgerMessage instance;

    public LedgerMessageBuilder(String sender, Message.Type type) {
        this.instance = new LedgerMessage(sender, type);
    }

    public LedgerMessageBuilder setFrom(String from) {
        instance.setFrom(from);
        return this;
    }

    public LedgerMessageBuilder setTo(String to) {
        instance.setTo(to);
        return this;
    }

    public LedgerMessageBuilder setValue(BigInteger value) {
        instance.setValue(value);
        return this;
    }

    public LedgerMessageBuilder setInputData(String inputData) {
        instance.setInputData(inputData);
        return this;
    }

    public LedgerMessageBuilder setResult(String result) {
        instance.setResult(result);
        return this;
    }

    public LedgerMessageBuilder setMessageId(int messageId) {
        instance.setMessageId(messageId);
        return this;
    }

    public LedgerMessageBuilder setSignature(String signature) {
        instance.setSignature(signature);
        return this;
    }

    public LedgerMessageBuilder setEpoch(int epoch) {
        instance.setEpoch(epoch);
        return this;
    }

    public LedgerMessageBuilder setCoinType(String coinType) {
        instance.setCoinType(coinType);
        return this;
    }

    public LedgerMessageBuilder setLogs(String logs) {
        instance.setLogs(logs);
        return this;
    }

    public LedgerMessageBuilder setWasOperationSuccessful(boolean wasOperationSuccessful) {
        instance.setWasOperationSuccessful(wasOperationSuccessful);
        return this;
    }
    
    public LedgerMessage build() {
        return instance;
    }
}