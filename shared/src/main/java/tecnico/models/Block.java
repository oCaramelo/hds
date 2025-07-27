package tecnico.models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Block {
    private String previousHash;
    private final String hash;
    private final List<Transaction> transactions;
    private final long timestamp;
    private final int nonce;
    private List<String> transactionLogs;
    private boolean isGenesisBlock;

    // Constructor for subsequent blocks
    public Block(String previousHash, String hash, List<Transaction> transactions) {

        this.previousHash = previousHash; // Genesis block has no previous hash
        this.hash = hash;
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
        this.transactions = transactions;
        this.isGenesisBlock = true;
    }

    public Block(List<Transaction> transactions) {

        this.previousHash = getPreviousHash();
        this.transactions = transactions;
        this.hash = calculateHash();
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
        this.isGenesisBlock = false;
    }

    // Method to calculate the block's hash using SHA-256
    public String calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = (previousHash != null ? previousHash : "") +
                          Long.toString(timestamp) +
                          Integer.toString(nonce) +
                          transactions.stream()
                                      .map(t -> t != null ? t.toString() : "")
                                      .collect(Collectors.joining());
            byte[] hashBytes = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "0x" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating hash: " + e.getMessage());
        }
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return hash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public List<String> getTransactionLogs() {
        return transactionLogs;
    }

    public void setTransactionLogs(List<String> transactionLogs) {
        this.transactionLogs = transactionLogs;
    }

    public boolean isGenesisBlock() {
        return isGenesisBlock;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block block = (Block) o;
        return Objects.equals(hash, block.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════════════════════════════════╗\n");

        if (isGenesisBlock) {
            sb.append(String.format("║ %-70s ║\n", ""));
            sb.append(String.format("║ %-70s ║\n", "                     █▀▀ █▀▀ █▄░█ █▀▀ █▀ █ █▀"));
            sb.append(String.format("║ %-70s ║\n", "                     █▄█ ██▄ █░▀█ ██▄ ▄█ █ ▄█"));
            sb.append(String.format("║ %-70s ║\n", ""));
            sb.append("╠════════════════════════════════════════════════════════════════════════╣\n");
        }

        sb.append(String.format("║ %-70s ║\n", "    Timestamp     : " + timestamp));
        sb.append(String.format("║ %-70s ║\n", "    Nonce         : " + nonce));
        sb.append(String.format("║ %-70s ║\n", "    Previous Hash : " + truncate(previousHash)));
        sb.append(String.format("║ %-70s ║\n", "    Hash          : " + truncate(hash)));
        sb.append("╠════════════════════════════════════════════════════════════════════════╣\n");

        if (transactions == null || transactions.isEmpty()) {
            sb.append(String.format("║ %-70s ║\n", ""));
            sb.append(String.format("║ %-70s ║\n", "[Transactions]  : (none)"));
            sb.append(String.format("║ %-70s ║\n", ""));
        } else {
            sb.append(String.format("║ %-70s ║\n", ""));
            sb.append(String.format("║ %-68s ║\n", "                  ¯\\_(ツ)_/¯ TRANSACTIONS ¯\\_(ツ)_/¯"));
            sb.append(String.format("║ %-70s ║\n", ""));
            for (Transaction t : transactions) {
                String[] lines = (t != null ? t.toString() : "null").split("\n");
                sb.append(String.format("║ %-70s ║\n", ""));
                for (String line : lines) {
                    if (!line.isEmpty())
                        sb.append(String.format("║ %-70s ║\n", "    " + line.trim()));
                }
                sb.append(String.format("║ %-70s ║\n", ""));
            }
            sb.append(String.format("║ %-70s ║\n", ""));
        }

        sb.append("╚════════════════════════════════════════════════════════════════════════╝\n\n");
        return sb.toString();
    }

    private String truncate(String str) {
        if (str == null) return "null";
        return str.length() <= 16 ? str : str.substring(0, 8) + "..." + str.substring(str.length() - 6);
    }

}