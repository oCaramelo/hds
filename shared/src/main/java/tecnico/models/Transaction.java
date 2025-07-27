package tecnico.models;

import java.math.BigInteger;

public class Transaction {
    private String from;
    private String to;
    private BigInteger value;
    private String inputData;
    private String coinType;
    private String transactionType;

    public Transaction(String from, String to, BigInteger value, String inputData, String coinType, String transactionType) {
        this.from = from;
        this.to = to;
        this.value = value;
        this.inputData = inputData;
        this.coinType = coinType;
        this.transactionType = transactionType;
    }

    public Transaction(String from, String inputData, String coinType, String transactionType) {
        this.from = from;
        this.inputData = inputData;
        this.coinType = coinType;
        this.transactionType = transactionType;
    }


    public String getFrom() { return from; }
    public String getTo() { return to; }
    public BigInteger getValue() { return value; }
    public String getInputData() { return inputData; }
    public String getCoinType() { return coinType; }
    public String getTransactionType() {return transactionType;}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        switch (transactionType) {
            case "Transfer":
                sb.append("[Transfer]\n");
                sb.append(from + "\n");
                sb.append("↓  " + coinType + ": " + value + "\n");
                sb.append(to + "\n");
                break;

            case "Balance":
                sb.append("\n");
                sb.append("[Balance]\n");
                sb.append(from + "\n");
                sb.append("↓\n");
                sb.append(coinType + "\n");
                sb.append("\n");
                break;

            default:
                sb.append("Unknown transaction type\n");
                break;
        }

        return sb.toString();
    }


}
