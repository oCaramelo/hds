package tecnico.models;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;

public class Blockchain {

    private final List<Block> blocks = new ArrayList<>();
    private final Map<String, Account> accounts = new HashMap<>();

    public synchronized void addBlock(Block block, EVMExecutor executor, ByteArrayOutputStream byteArrayOutputStream, Map<String, Bytes> contractsBytecode) {
        String previousHash = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1).getHash();
        
        block.setPreviousHash(previousHash);

        blocks.add(block);
        applyBlockTransactions(block, executor, byteArrayOutputStream, contractsBytecode);
    }

    private void applyBlockTransactions(Block block, EVMExecutor executor, ByteArrayOutputStream byteArrayOutputStream, Map<String, Bytes> contractsBytecode) {
        List<Transaction> transactions = block.getTransactions();
        List<String> transactionLogs = new ArrayList<>();
        Boolean isGenesisBlock = block.isGenesisBlock();

        for (Transaction tx : transactions) {
            String to = tx.getTo();
            String from = tx.getFrom();
            BigInteger value = tx.getValue();
            String coinType = tx.getCoinType();
            String inputData = tx.getInputData();
            String transactionType = tx.getTransactionType();
            

            if (transactionType.equals("Transfer")) {
                transactionLogs.add(applyTransferTransaction(from, to, value, coinType, inputData, isGenesisBlock, executor, byteArrayOutputStream, contractsBytecode, accounts));
            } else if (transactionType.equals("Balance")) {
                transactionLogs.add(applyBalanceTransaction(from, coinType, inputData, isGenesisBlock, executor, byteArrayOutputStream, contractsBytecode, accounts));

            }

        block.setTransactionLogs(transactionLogs);

        }
    }

    private String applyTransferTransaction(String from, String to, BigInteger value, String coinType, String inputData, boolean isGenesisBlock,
            EVMExecutor executor, ByteArrayOutputStream byteArrayOutputStream, Map<String, Bytes> contractsBytecode, Map<String, Account> accounts) {
        
        if ("DepCoin".equals(coinType)) {

            if (isGenesisBlock){
                accounts.putIfAbsent(to, new Account(to));
                accounts.get(to).setDepCoinBalance(value);
                return "Genesis transfer of " + value + " DepCoin to " + to + " successful.";
            }

            accounts.get(from).subtractDepCoin(value);
            accounts.get(to).addDepCoin(value);

            return "Transfer of " + value + " DepCoin from " + from + " to " + to + " successful.";

        } else if ("ISTCoin".equals(coinType)) {
            try {
                executor.callData(Bytes.fromHexString(inputData));
                executor.sender(Address.fromHexString((from)));
                executor.code(contractsBytecode.get(coinType));
                executor.execute();

                boolean success = extractBooleanFromReturnData(byteArrayOutputStream);
                if (success) {
                    return "Transfer of " + value + " ISTCoin from " + from + " to " + to + " successful.";

                } else {
                    return "ISTCoin transfer failed";
                }
            } catch (Exception e) {
                return "ISTCoin transfer failed";
            }
        }

        return "Unknown transaction type";
    }

    private String applyBalanceTransaction(String from, String coinType, String inputData, boolean isGenesisBlock, EVMExecutor executor,
            ByteArrayOutputStream byteArrayOutputStream, Map<String, Bytes> contractsBytecode, Map<String, Account> accounts) {
        if (coinType.equals("DepCoin")) {
            Account fromAccount = accounts.get(from);
            return "DepCoin balance for " + from + ": " + fromAccount.getDepCoinBalance();
        }
        else if (coinType.equals("ISTCoin")) {
            try {
                executor.callData(Bytes.fromHexString(inputData));
                executor.code(contractsBytecode.get(coinType));
                executor.execute();

                BigInteger balance = extractBigIntFromReturnData(byteArrayOutputStream);

                if (isGenesisBlock) {
                    accounts.putIfAbsent(from, new Account(from));
                }

                accounts.get(from).setIstCoinBalance(balance);
                return "ISTCoin balance for " + from + ": " + balance;
            } catch (Exception e) {
                return "Failed to retrieve ISTCoin balance: " + e.getMessage();
            }
        }
        return "Unknown coin type";
    }


    public synchronized List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public static boolean extractBooleanFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        try {
            String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
            JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

            String memory = jsonObject.get("memory").getAsString();
            JsonArray stack = jsonObject.get("stack").getAsJsonArray();

            int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
            int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

            String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
            return Boolean.parseBoolean(returnData);
        } catch (Exception e) {
            System.err.println("Failed to extract boolean from return data: " + e.getMessage());
            return false;
        }
    }

    public static BigInteger extractBigIntFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        try {
            String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
            JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

            String memory = jsonObject.get("memory").getAsString();
            JsonArray stack = jsonObject.get("stack").getAsJsonArray();
            int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
            int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

            // Validate size for uint256 (32 bytes = 64 hex chars)
            if (size != 32) {
                throw new IllegalStateException("Expected size 32 for uint256, got " + size);
            }

            // Extract return data
            String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

            // Validate return data
            if (!returnData.matches("[0-9a-fA-F]{64}")) {
                throw new IllegalStateException("Invalid return data: " + returnData);
            }

            return new BigInteger(returnData, 16);
        } catch (Exception e) {
            System.err.println("Failed to extract BigInteger from return data: " + e.getMessage());
            throw new RuntimeException("Error extracting ISTCoin balance", e);
        }
    }
    public Account getAccount(String accountAddress) {
        return accounts.get(accountAddress);
    }

}
