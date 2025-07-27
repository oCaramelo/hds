package tecnico.communication;

import java.math.BigInteger;

public class LedgerMessage extends Message {
    private String fromId;
    private String from;
    private String to;
    private BigInteger value;
    private String inputData;
    private String result;
    private String logs;
    private boolean wasOperationSuccessful;

    private String coinType;

    public LedgerMessage(String senderId, Type type) {
        super(senderId, type);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getCoinType() {
        return coinType;
    }

    public void setCoinType(String coinType) {
        this.coinType = coinType;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public boolean wasOperationSuccessful() {
        return wasOperationSuccessful;
    }

    public void setWasOperationSuccessful(boolean wasOperationSuccessful) {
        this.wasOperationSuccessful = wasOperationSuccessful;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LedgerMessage{")
        .append("type=").append(getType())
        .append(", senderId=").append(getSenderId());

        if (fromId != null) sb.append(", fromId=").append(fromId);
        if (from != null) sb.append(", from=").append(from);
        if (to != null) sb.append(", to=").append(to);
        if (value != null) sb.append(", value=").append(value);
        if (inputData != null) sb.append(", inputData=").append(inputData);
        if (result != null) sb.append(", result=").append(result);

        sb.append("}");
        return sb.toString();
    }
}