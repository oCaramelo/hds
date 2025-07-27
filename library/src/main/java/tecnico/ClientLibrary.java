package tecnico;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import tecnico.services.UDPService;
import tecnico.communication.AuthenticatedPerfectLink;
import tecnico.communication.Message;
import tecnico.communication.LedgerMessage;
import tecnico.communication.builder.LedgerMessageBuilder;
import tecnico.logger.CustomLogger;
import tecnico.configs.ClientConfig;
import tecnico.configs.ProcessConfig;
import tecnico.exceptions.HDSSException;
import java.math.BigInteger;


public class ClientLibrary implements UDPService {

    private final ProcessConfig[] clientConfigs;
    private final ProcessConfig[] nodeConfigs;
    private final ProcessConfig config;
    private final ClientMessageBucket messageBucket;
    private final ConcurrentSkipListMap<Integer, Boolean> completedEpochs = new ConcurrentSkipListMap<>();
    private final AuthenticatedPerfectLink<LedgerMessage> link;
    private final CustomLogger logger;
    private boolean wasLedgerShown = false;
    private boolean wasNetworkShown = false;

    public ClientLibrary(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs) throws HDSSException {
        this.nodeConfigs = nodeConfigs;
        this.clientConfigs = clientConfigs;
        this.config = clientConfig;
        this.messageBucket = new ClientMessageBucket(nodeConfigs.length);
        this.logger = new CustomLogger(this.getClass().getName());
        this.link = new AuthenticatedPerfectLink(config, config.getPort(), nodeConfigs, LedgerMessage.class);
    }

    public void transfer(String from, String to, BigInteger value, boolean isISTCoin) {
        try {
            LedgerMessageBuilder builder = new LedgerMessageBuilder(this.config.getId(), Message.Type.TRANSFER)
                .setFrom(from)
                .setTo(to)
                .setValue(value);
                if (isISTCoin) {
                    builder.setInputData(encodeTransferABI(to, value));
                    builder.setCoinType("ISTCoin");
                } else {
                    builder.setInputData("");
                    builder.setCoinType("DepCoin");
                }
            
            LedgerMessage message = builder.build();
            link.broadcast(message);
        } catch (Exception e) {
            logger.error(MessageFormat.format("[CLIENT_LIBRARY] [ERROR] Error sending TRANSFER message: {0}", e.getMessage()));
        }
    }

    public void showProfile(String clientAddress) {
        try {
            LedgerMessage message = new LedgerMessageBuilder(this.config.getId(), Message.Type.SHOW_PROFILE)
                .setFrom(clientAddress)
                .setInputData(encodeBalanceOfABI(clientAddress))
                .build();
            link.broadcast(message);
        } catch (Exception e) {
            logger.error(MessageFormat.format("[CLIENT_LIBRARY] [ERROR] Error sending SHOW_PROFILE message: {0}", e.getMessage()));
        }
    }

    public void showBlockchain(String clientAddress) {
        wasLedgerShown = false;
        try {
            LedgerMessage message = new LedgerMessageBuilder(this.config.getId(), Message.Type.SHOW_BLOCKCHAIN)
                .setFrom(clientAddress)
                .build();
            link.broadcast(message);
        } catch (Exception e) {
            logger.error(MessageFormat.format("[CLIENT_LIBRARY] [ERROR] Error sending SHOW_BLOCKCHAIN message: {0}", e.getMessage()));
        }
    }

    public void showNetwork(String clientAddress) {
        wasNetworkShown = false;
        try {
            LedgerMessage message = new LedgerMessageBuilder(this.config.getId(), Message.Type.SHOW_NETWORK)
                .setFrom(clientAddress)
                .build();
            link.broadcast(message);
        } catch (Exception e) {
            logger.error(MessageFormat.format("[CLIENT_LIBRARY] [ERROR] Error sending SHOW_NETWORK message: {0}", e.getMessage()));
        }
    }

    private void handleTransferResult(LedgerMessage message) {
        int epoch = message.getEpoch();

        if (completedEpochs.containsKey(epoch)) {
            return;
        }

        messageBucket.addMessage(message);

        Optional<LedgerMessage> consensusMessage = messageBucket.hasSufficientSuccessfulResultMessage(
            epoch, Message.Type.TRANSFER_RESULT);

        if (consensusMessage.isPresent()) {
            synchronized (this) {
                if (!completedEpochs.containsKey(epoch)) {
                    LedgerMessage msg = consensusMessage.get();

                    if (msg.wasOperationSuccessful()) {
                        logger.info(MessageFormat.format(
                            "✅ [CLIENT_LIBRARY] Transfer confirmed for epoch {0}",
                            epoch
                        ));
                    } else {
                        logger.info(MessageFormat.format(
                            "❌ [CLIENT_LIBRARY] Transfer failed for epoch {0}:\n{1}",
                            epoch,
                            msg.getResult()
                        ));
                    }

                    completedEpochs.put(epoch, true);
                    cleanOldEpochs(epoch);
                }
            }
        }
    }


    private void handleShowProfileResult(LedgerMessage message) {
        int epoch = message.getEpoch();

        if (completedEpochs.containsKey(epoch)) {
            return;
        }

        messageBucket.addMessage(message);

        Optional<LedgerMessage> consensusMessage = messageBucket.hasSufficientSuccessfulResultMessage(epoch, Message.Type.SHOW_PROFILE_RESULT);

        if (consensusMessage.isPresent()) {
            synchronized (this) {
                if (!completedEpochs.containsKey(epoch)) {
                    LedgerMessage msg = consensusMessage.get();

                    if (msg.wasOperationSuccessful()) {
                        String result = msg.getResult();
                        String depCoinStr = null;
                        String istCoinStr = null;

                        try {
                            String[] parts = result.split(",");
                            for (String part : parts) {
                                part = part.trim();
                                if (part.startsWith("DepCoin:")) {
                                    depCoinStr = part.substring("DepCoin:".length()).trim();
                                } else if (part.startsWith("ISTCoin:")) {
                                    istCoinStr = part.substring("ISTCoin:".length()).trim();
                                }
                            }

                            String frame = ""
                                + "╔════════════════════════════════════════════════════════════════════════╗\n"
                                + String.format("║ %-70s ║\n", "")
                                + String.format("║ %-70s ║\n", "         ██████╗ ██████╗  ██████╗ ███████╗██╗██╗     ███████╗")
                                + String.format("║ %-70s ║\n", "         ██╔══██╗██╔══██╗██╔═══██╗██╔════╝██║██║     ██╔════╝")
                                + String.format("║ %-70s ║\n", "         ██████╔╝██████╔╝██║   ██║█████╗  ██║██║     █████╗  ")
                                + String.format("║ %-70s ║\n", "         ██╔═══╝ ██╔══██╗██║   ██║██╔══╝  ██║██║     ██╔══╝  ")
                                + String.format("║ %-70s ║\n", "         ██║     ██║  ██║╚██████╔╝██║     ██║███████╗███████╗")
                                + String.format("║ %-70s ║\n", "         ╚═╝     ╚═╝  ╚═╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝╚══════╝")
                                + String.format("║ %-70s ║\n", "")
                                + "╠════════════════════════════════════════════════════════════════════════╣\n"
                                + String.format("║ %-70s ║\n", "")
                                + String.format("║ %-70s ║\n", "   [Client ID]: " + config.getId())
                                + String.format("║ %-70s ║\n", "")
                                + String.format("║ %-70s ║\n", "   [Address]: ")
                                + String.format("║ %-70s ║\n", "   " + message.getFrom())
                                + String.format("║ %-70s ║\n", "")
                                + String.format("║ %-69s ║\n", "   [Balance]:                               ¯\\_(ツ)_/¯")
                                + String.format("║ %-70s ║\n", "    - DepCoin: " + depCoinStr)
                                + String.format("║ %-70s ║\n", "    - ISTCoin: " + istCoinStr)
                                + String.format("║ %-70s ║\n", "")
                                + "╚════════════════════════════════════════════════════════════════════════╝";

                            logger.info(frame);
                            
                        } catch (Exception e) {
                            logger.info("⚠️ [CLIENT_LIBRARY] Failed to parse profile result: " + result);
                        }

                    } else {
                        logger.info(MessageFormat.format(
                            "❌ [CLIENT_LIBRARY] Transfer failed for epoch {0}:\n{1}",
                            epoch,
                            msg.getResult()
                        ));
                    }

                    completedEpochs.put(epoch, true);
                    cleanOldEpochs(epoch);
                }
            }
        }
    }

    private void handleShowBlockchainResult(LedgerMessage message) {
        int epoch = message.getEpoch();

        messageBucket.addMessage(message);

        Optional<LedgerMessage> consensusMessage = messageBucket.hasSufficientSuccessfulResultMessage(
            epoch, Message.Type.SHOW_BLOCKCHAIN_RESULT);

        if (consensusMessage.isPresent() && !wasLedgerShown) {
            synchronized (this) {

                LedgerMessage msg = consensusMessage.get();

                String blockchainHeader =
                        "\n" +
                        "██████╗ ██╗      ██████╗  ██████╗██╗  ██╗ ██████╗██╗  ██╗ █████╗ ██╗███╗   ██╗\n" +
                        "██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝██╔════╝██║  ██║██╔══██╗██║████╗  ██║\n" +
                        "██████╔╝██║     ██║   ██║██║     █████╔╝ ██║     ███████║███████║██║██╔██╗ ██║\n" +
                        "██╔══██╗██║     ██║   ██║██║     ██╔═██╗ ██║     ██╔══██║██╔══██║██║██║╚██╗██║\n" +
                        "██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗╚██████╗██║  ██║██║  ██║██║██║ ╚████║\n" +
                        "╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝\n";

                logger.info(blockchainHeader);
                logger.info(MessageFormat.format(
                    "                       Blockchain for epoch {0}\n{1}\n",
                    epoch,
                    msg.getResult()
                ));

                wasLedgerShown = true;
            }
        }
    }

    private void handleShowNetworkResult(LedgerMessage message) {
        int epoch = message.getEpoch();

        messageBucket.addMessage(message);

        Optional<LedgerMessage> consensusMessage =
            messageBucket.hasSufficientSuccessfulResultMessage(epoch, Message.Type.SHOW_NETWORK_RESULT);

        if (consensusMessage.isPresent() && !wasNetworkShown) {
            synchronized (this) {
                LedgerMessage msg = consensusMessage.get();
                String rawResult = msg.getResult();
                String from = msg.getFrom();

                List<String> addresses = Arrays.stream(
                        rawResult.replace("[", "")
                                .replace("]", "")
                                .split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

                List<String> sorted = new ArrayList<>();
                if (addresses.contains(from)) {
                    sorted.add(from);
                }
                for (String address : addresses) {
                    if (!address.equals(from)) {
                        sorted.add(address);
                    }
                }

                StringBuilder formatted = new StringBuilder();

                formatted.append("╔════════════════════════════════════════════════════════════════════════╗\n");
                formatted.append(String.format("║ %-70s ║\n", ""));
                formatted.append(String.format("║ %-70s ║\n", "    ███╗   ██╗███████╗████████╗██╗    ██╗ ██████╗ ██████╗ ██╗  ██╗"));
                formatted.append(String.format("║ %-70s ║\n", "    ████╗  ██║██╔════╝╚══██╔══╝██║    ██║██╔═══██╗██╔══██╗██║ ██╔╝"));
                formatted.append(String.format("║ %-70s ║\n", "    ██╔██╗ ██║█████╗     ██║   ██║ █╗ ██║██║   ██║██████╔╝█████╔╝ "));
                formatted.append(String.format("║ %-70s ║\n", "    ██║╚██╗██║██╔══╝     ██║   ██║███╗██║██║   ██║██╔══██╗██╔═██╗ "));
                formatted.append(String.format("║ %-70s ║\n", "    ██║ ╚████║███████╗   ██║   ╚███╔███╔╝╚██████╔╝██║  ██║██║  ██╗"));
                formatted.append(String.format("║ %-70s ║\n", "    ╚═╝  ╚═══╝╚══════╝   ╚═╝    ╚══╝╚══╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝"));
                formatted.append(String.format("║ %-70s ║\n", ""));
                formatted.append("╠════════════════════════════════════════════════════════════════════════╣\n");
                formatted.append(String.format("║ %-70s ║\n", " [USERS]"));
                formatted.append(String.format("║ %-70s ║\n", ""));

                for (String address : sorted) {
                    String label = address.equals(from) ? "    - " + address + "    [ YOU ]" : "    - " + address;
                    formatted.append(String.format("║ %-70s ║\n", label));
                }

                formatted.append("╚════════════════════════════════════════════════════════════════════════╝");

                logger.info(formatted.toString());

                wasNetworkShown = true;
            }
        }
    }

    private void cleanOldEpochs(int newlyCompletedEpoch) {
        int lowestContiguous = newlyCompletedEpoch;
        while (completedEpochs.containsKey(lowestContiguous - 1)) {
            lowestContiguous--;
        }

        if (lowestContiguous < newlyCompletedEpoch) {
            completedEpochs.headMap(lowestContiguous).clear();
            messageBucket.clearEpochsBelow(lowestContiguous);
        }
    }

    public String encodeBalanceOfABI(String address) {
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(address)),
            Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    public String encodeTransferABI(String to, BigInteger value) {
        Function function = new Function(
            "transfer",
            Arrays.asList(new Address(to), new Uint256(value)),
            Collections.emptyList()
        );
        return FunctionEncoder.encode(function);
    }


    @Override
    public void listen() {
        try {
            new Thread(() -> {
                try {
                    while (true) {
                        LedgerMessage message = link.receive();
                        switch (message.getType()) {
                            case TRANSFER_RESULT -> handleTransferResult(message);
                            case SHOW_PROFILE_RESULT -> handleShowProfileResult(message);
                            case SHOW_BLOCKCHAIN_RESULT -> handleShowBlockchainResult(message);
                            case SHOW_NETWORK_RESULT -> handleShowNetworkResult(message);
                            case ACK -> {
                                //logger.info(MessageFormat.format("[CLIENT_LIBRARY] Received ACK response from {0}", message.getSenderId()));
                            }
                            case IGNORE ->
                                logger.info(MessageFormat.format("[CLIENT_LIBRARY] Received IGNORE response from {0}", message.getSenderId()));
                            default ->
                                logger.info(MessageFormat.format("[CLIENT_LIBRARY] Received UNKNOWN response from {0}", message.getSenderId()));
                        }
                    }
                } catch (IOException e) {
                    logger.error(MessageFormat.format("[CLIENT_LIBRARY] [ERROR] Error receiving message: {0}", e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            logger.error(MessageFormat.format("[CLIENT_LIBRARY] [ERROR] Error starting listener: {0}", e.getMessage()));
        }
    }
}
