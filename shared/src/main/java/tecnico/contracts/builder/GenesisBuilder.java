package tecnico.contracts.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tecnico.models.Block;
import tecnico.models.Transaction;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GenesisBuilder {

    private final SimpleWorld simpleWorld;
    private EVMExecutor executor;
    private ByteArrayOutputStream byteArrayOutputStream;
    private StandardJsonTracer tracer;
    private MutableAccount contractAccount;

    public GenesisBuilder() {
        this.simpleWorld = new SimpleWorld();
    }

    public SimpleWorld getSimpleWorld() {
        return simpleWorld;
    }

    public EVMExecutor initialize() {
        try {
            // Read genesis.json from resources
            InputStream inputStream = GenesisBuilder.class.getClassLoader().getResourceAsStream("genesis.json");
            if (inputStream == null) {
                throw new IllegalArgumentException("genesis.json not found in resources");
            }

            // Parse genesis.json
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject genesis = JsonParser.parseString(jsonContent).getAsJsonObject();

            // Process accounts from genesis
            JsonObject accounts = genesis.getAsJsonObject("accounts");
            for (String address : accounts.keySet()) {
                JsonObject accountData = accounts.getAsJsonObject(address);
                String type = accountData.get("type").getAsString();

                if ("SYSTEM".equals(type)) {
                    continue;
                }

                String balance = accountData.get("balance").getAsString();

                Address accountAddress = Address.fromHexString(address);
                Wei accountBalance = Wei.of(new java.math.BigInteger(balance));

                // Create account
                simpleWorld.createAccount(accountAddress, 0, accountBalance);
                MutableAccount account = (MutableAccount) simpleWorld.get(accountAddress);

                // System.out.println(type + " Account");
                // System.out.println("  Address: " + account.getAddress());
                // System.out.println("  Balance: " + account.getBalance());
                // System.out.println("  Nonce: " + account.getNonce());

                // Handle contract-specific data
                if ("CONTRACT".equals(type)) {
                    String code = accountData.get("code").getAsString();
                    account.setCode(Bytes.fromHexString(code));
                }
                // System.out.println();
            }

            // Setup debugging tracer
            byteArrayOutputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(byteArrayOutputStream);
            tracer = new StandardJsonTracer(printStream, true, true, true, true);

            // Setup EVM executor
            Address senderAddress = Address.fromHexString(accounts.keySet().stream()
                .filter(k -> accounts.getAsJsonObject(k).get("type").getAsString().equals("EOA"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No EOA account found")));
            Address contractAddress = Address.fromHexString(accounts.keySet().stream()
                .filter(k -> accounts.getAsJsonObject(k).get("type").getAsString().equals("CONTRACT"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No CONTRACT account found")));
            
            // System.out.println("[DEBUG] " + Bytes.fromHexString(accounts.getAsJsonObject(contractAddress.toHexString()).get("code").getAsString()));
            var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
            executor.tracer(tracer);
            executor.code(Bytes.fromHexString(accounts.getAsJsonObject(contractAddress.toHexString()).get("code").getAsString()));
            executor.sender(senderAddress); // Client 1 address
            executor.worldUpdater(simpleWorld.updater());
            executor.commitWorldState();
            executor.callData(Bytes.EMPTY);
            executor.execute(); // Execute constructor

            // Extract and set the run time bytecode on the contract account
            String runtimeByteCode = extractStringFromReturnData(byteArrayOutputStream);
            contractAccount = (MutableAccount) simpleWorld.get(contractAddress);
            contractAccount.setCode(Bytes.fromHexString(runtimeByteCode));

            // System.out.println("[DEBUG] Contract Runtime Bytecode: " + runtimeByteCode);

            return executor;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        // System.out.println("[DEBUG] " + byteArrayOutputStream.toString(StandardCharsets.UTF_8));
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        return memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
    }

    public Block buildGenesisBlock() {
        List<Transaction> transactions = new ArrayList<>();
        String systemAddress = "";
        String smartContractSystemAddress = "";

        try {
            InputStream inputStream = GenesisBuilder.class.getClassLoader().getResourceAsStream("genesis.json");
            if (inputStream == null) {
                throw new IllegalArgumentException("genesis.json not found in resources");
            }

            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject genesis = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonObject accountsJson = genesis.getAsJsonObject("accounts");

            String previousHash = genesis.has("previous_block_hash") && !genesis.get("previous_block_hash").isJsonNull()
                    ? genesis.get("previous_block_hash").getAsString()
                    : null;

            String blockHash = genesis.has("block_hash")
                    ? genesis.get("block_hash").getAsString()
                    : null;

            for (String address : accountsJson.keySet()) {
                JsonObject accountJson = accountsJson.getAsJsonObject(address);
                if (accountJson.has("type") && accountJson.get("type").getAsString().equals("SYSTEM")) {
                    systemAddress = address;
                    break;
                }
            }

            if (systemAddress.isEmpty()) {
                throw new IllegalStateException("No system account found in genesis.json");
            }

            for (String address : accountsJson.keySet()) {
                JsonObject accountJson = accountsJson.getAsJsonObject(address);
                if (accountJson.has("type") && accountJson.get("type").getAsString().equals("CONTRACT")) {
                    smartContractSystemAddress = address;
                    break;
                }
            }

            if (smartContractSystemAddress.isEmpty()) {
                throw new IllegalStateException("No smart contract account found in genesis.json");
            }

            for (String address : accountsJson.keySet()) {
                JsonObject accountJson = accountsJson.getAsJsonObject(address);
                
                if (accountJson.has("type") && accountJson.get("type").getAsString().equals("EOA")) {
                    BigInteger depCoinBalance = new BigInteger(accountJson.get("balance").getAsString());
                    if (depCoinBalance.compareTo(BigInteger.ZERO) > 0) {
                        transactions.add(new Transaction(
                                systemAddress,
                                address,
                                depCoinBalance,
                                "",
                                "DepCoin",
                                "Transfer"
                        ));
                    }

                    transactions.add(new Transaction(
                        address,
                        encodeBalanceOfABI(address),
                        "ISTCoin",
                        "Balance"
                    ));
                }
            }

            return new Block(previousHash, blockHash, transactions);
        } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error building genesis block: " + e.getMessage());
        }
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
        return byteArrayOutputStream;
    }

    public MutableAccount getContractAccount() {
        return contractAccount;
    }
    public String encodeBalanceOfABI(String address) {
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new org.web3j.abi.datatypes.Address(address)),
            Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }
}