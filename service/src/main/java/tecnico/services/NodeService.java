package tecnico.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.hyperledger.besu.evm.fluent.EVMExecutor;

import tecnico.communication.*;
import tecnico.communication.builder.*;
import tecnico.configs.*;
import tecnico.contracts.builder.GenesisBuilder;
import tecnico.logger.CustomLogger;
import tecnico.models.*;
import tecnico.encryption.EncryptionTools;
import tecnico.utils.ByzantineUtils;
import org.apache.tuweni.bytes.Bytes;

public class NodeService implements UDPService {

    private static final CustomLogger logger = new CustomLogger(NodeService.class.getName());

    private final NodeConfig[] nodesConfig;
    private final NodeConfig config;
    private final NodeConfig leaderConfig;
    private String senderID;
    private final Map<String, ProcessConfig> nodes = new ConcurrentHashMap<>();

    private volatile Runnable consensusCompletionCallback;

    private final AuthenticatedPerfectLink<ConsensusMessage> link;
    private final AuthenticatedPerfectLink<LedgerMessage> linkToClients;

    private final int quorumSize;
    private final int f;
    private final int nodeCount;

    private final Blockchain blockchain = new Blockchain();
    private final EVMExecutor executor;
    private final ByteArrayOutputStream byteArrayOutputStream;
    private Map<String, Bytes> contractsBytecode = new HashMap<>();
    private Message.Type clientMessageType;
    private String clientAddress;
    private String from = "";
    private String to = "";

    private final AtomicInteger consensusEpoch = new AtomicInteger(0);

    private int currentEpoch = -1;
    private Block currentBlock = null;
    private final Map<Integer, Block> writeset = new ConcurrentHashMap<>();
    private Block leaderProposedBlock = null;

    private Boolean conditionalCollectDone = false;
    private Boolean writePhaseDone = false;
    private Boolean valueHasDecided = false;

    private final StateMessageBucket stateMessageBucket;
    private final WriteMessageBucket writeMessageBucket;
    private final AcceptMessageBucket acceptMessageBucket;

    public NodeService(AuthenticatedPerfectLink<ConsensusMessage> link, AuthenticatedPerfectLink<LedgerMessage> linkToClients, NodeConfig config, NodeConfig[] nodesConfig) {
        this.link = link;
        this.linkToClients = linkToClients;
        this.config = config;
        this.nodesConfig = nodesConfig;
        this.leaderConfig = Arrays.stream(nodesConfig).filter(NodeConfig::isLeader).findAny().get();

        this.stateMessageBucket = new StateMessageBucket(nodesConfig.length);
        this.writeMessageBucket = new WriteMessageBucket(nodesConfig.length);
        this.acceptMessageBucket = new AcceptMessageBucket(nodesConfig.length);

        Arrays.stream(nodesConfig).forEach(node -> this.nodes.put(node.getId(), node));

        this.nodeCount = nodesConfig.length;
        this.f = (nodeCount - 1) / 3;
        this.quorumSize = (nodeCount + f) / 2 + 1;

        GenesisBuilder genesisBuilder = new GenesisBuilder();
        executor = genesisBuilder.initialize();
        Block genesisBlock = genesisBuilder.buildGenesisBlock();
        byteArrayOutputStream = genesisBuilder.getByteArrayOutputStream();
        contractsBytecode.put("ISTCoin", genesisBuilder.getContractAccount().getCode()) ;
        appendToBlockchain(genesisBlock);

    }

    public void startConsensus(LedgerMessage clientMessage, Block block, Runnable onCompletion) {
        this.currentEpoch = consensusEpoch.getAndIncrement();
        this.currentBlock = block;
        this.senderID = clientMessage.getSenderId();
        this.consensusCompletionCallback = onCompletion;
        this.clientMessageType = clientMessage.getType();
        this.clientAddress = clientMessage.getFrom();
        this.from = clientMessage.getFrom();
        this.to = clientMessage.getTo();

        this.conditionalCollectDone = false;
        this.writePhaseDone = false;
        this.valueHasDecided = false;

        if (config.isLeader()) {
            ConsensusMessage readMessage = new ConsensusMessageBuilder(config.getId(), ConsensusMessage.Type.READ)
                .setEpoch(currentEpoch)
                .setProposedBlock(block)
                .setLeaderId(config.getId())
                .build();

            try {
                String signature = EncryptionTools.sign(readMessage.toString(), config.getPrivateKeyPath());
                readMessage.setSignature(signature);
            } catch (Exception e) {
                logger.log(Level.INFO, "❌ [SIGN_ERROR] Failed to sign READ message: " + e.getMessage());
                triggerTimeout();
                return;
            }

            link.broadcast(readMessage);
        }
    }

    private void handleRead(ConsensusMessage message) {

        logger.log(Level.INFO, "[NODE_SERVICE] [HANDLE_READ] : Handling READ message from " + message.getSenderId());

        if (!EncryptionTools.verifySignature(message.toString(), message.getSignature(), nodes.get(message.getSenderId()).getPublicKeyPath())) {
            logger.log(Level.INFO, "⚠️ [NODE_SERVICE] [INVALID_SIGNATURE] READ message from " + message.getSenderId());
            triggerTimeout();
            return;
        }

        // Store the leader's proposed value cause only the leader can send READ messages
        this.leaderProposedBlock= message.getProposedBlock();

        writeset.put(currentEpoch, currentBlock);

        // Respond with a STATE message containing the current state
        ConsensusMessage stateMessage = new ConsensusMessageBuilder(config.getId(), ConsensusMessage.Type.STATE)
                 .setEpoch(currentEpoch)
                 .setLeaderId(leaderConfig.getId())
                 .setProposedBlock(currentBlock)
                 .setWriteset(writeset)
                 .build();

        try {
            String signature = EncryptionTools.sign(stateMessage.toString(), config.getPrivateKeyPath());
            stateMessage.setSignature(signature);
        } catch (Exception e) {
            logger.log(Level.INFO, "❌ [SIGN_ERROR] Failed to sign STATE message: " + e.getMessage());
            triggerTimeout();
            return;
        }

        link.broadcast(stateMessage);
    }

    private void handleState(ConsensusMessage message) {
        logger.log(Level.INFO, "[NODE_SERVICE] [HANDLE_READ] : Handling STATE message from " + message.getSenderId());

        if (!EncryptionTools.verifySignature(message.toString(), message.getSignature(), nodes.get(message.getSenderId()).getPublicKeyPath())) {
            logger.log(Level.INFO, "⚠️ [NODE_SERVICE] [INVALID_SIGNATURE] STATE message from " + message.getSenderId());
            triggerTimeout();
            return;
        }

        // Add the message to the STATE bucket
        stateMessageBucket.addMessage(message);

        // Check if there is a valid quorum for STATE messages
        Optional<Block> quorumValue = stateMessageBucket.hasValidStateQuorum(message.getEpoch());
        if (quorumValue.isPresent() && !conditionalCollectDone) {
            System.out.println("[NODE_SERVICE] [LEADER] [CONDITIONAL_COLLECT] : Valid STATE quorum found");
            conditionalCollectDone = true;
            // Collect the states of the processes
            List<ConsensusMessage> states = stateMessageBucket.getStates(message.getEpoch());

            logger.log(Level.INFO, "[DEBUG] [STATES]: " + states);

            // Execute the Conditional Collect to check if there is a valid value
            runConditionalCollect(states);
        }

        // logger.log(Level.INFO, "[NODE_SERVICE] [LEADER] [INFO] : No valid STATE quorum found (yet, waiting...)");
    }

    // Conditional Collect Logic

    private void runConditionalCollect(List<ConsensusMessage> states) {

        // Check if sounds()

        if (binds(states)) {
            logger.log(Level.WARNING, "[NODE_SERVICE] [LEADER] [CONDITIONAL_COLLECT]: BINDS");
            // Decide to write the valid value
            Block decidedBlock = states.get(0).getProposedBlock();
            logger.log(Level.INFO, "Decided block: " + decidedBlock);

            // Update the state and broadcast WRITE message
            currentEpoch = states.get(0).getEpoch();
            currentBlock = decidedBlock;

            ConsensusMessage writeMessage = new ConsensusMessageBuilder(config.getId(), ConsensusMessage.Type.WRITE)
                    .setEpoch(currentEpoch)
                    .setLeaderId(leaderConfig.getId())
                    .setProposedBlock(currentBlock)
                    .build();

            try {
                String signature = EncryptionTools.sign(writeMessage.toString(), config.getPrivateKeyPath());
                writeMessage.setSignature(signature);
            } catch (Exception e) {
                logger.log(Level.INFO, "❌ [SIGN_ERROR] Failed to sign WRITE message: " + e.getMessage());
                triggerTimeout();
                return;
            }

            link.broadcast(writeMessage);

        } else if (unbound(states)) {

            logger.log(Level.INFO, "[NODE_SERVICE] [LEADER] [CONDITIONAL_COLLECT] : UNBOUND");

            // Decide to write the value proposed by the leader
            currentBlock = leaderProposedBlock;

            ConsensusMessage writeMessage = new ConsensusMessageBuilder(config.getId(), ConsensusMessage.Type.WRITE)
                    .setEpoch(currentEpoch)
                    .setLeaderId(leaderConfig.getId())
                    .setProposedBlock(currentBlock)
                    .build();

            try {
                String signature = EncryptionTools.sign(writeMessage.toString(), config.getPrivateKeyPath());
                writeMessage.setSignature(signature);
            } catch (Exception e) {
                logger.log(Level.INFO, "❌ [SIGN_ERROR] Failed to sign WRITE message: " + e.getMessage());
                triggerTimeout();
                return;
            }

            link.broadcast(writeMessage);
        }
        else {
            logger.log(Level.WARNING, "[NODE_SERVICE] [WARNING] : Value not sound (Inconsistent with writesets)");
        }
    }

    private boolean sound(List<ConsensusMessage> states) {
        return binds(states) || unbound(states);
    }

    private boolean binds(List<ConsensusMessage> states) {

        // find(highestTs) +  find(value) -> (highestTs, value)

        int highestTs = states.stream()
                            .mapToInt(ConsensusMessage::getEpoch)
                            .max()
                            .orElse(-1);


        // logger.log(Level.INFO, "[DEBUG] [HIGHEST_TS]: " + highestTs);

        if (highestTs == 0) {
            return false;
        }

        
        Block value = states.stream()
                           .filter(message -> message.getEpoch() == highestTs)
                           .map(ConsensusMessage::getProposedBlock)
                           .findFirst()
                           .orElse(null);

        // logger.log(Level.INFO, "[DEBUG] [HIGHEST_VALUE]: " + value);
    
        if (value == null) {
            return false;
        }
    
        boolean isQuorumHighest = quorumHighest(highestTs, value, states);

        // logger.log(Level.INFO, "[DEBUG] [ISQUORUMHIGHEST]: " + isQuorumHighest);
    
        boolean isCertifiedValue = certifiedValue(highestTs, value, states);

        // logger.log(Level.INFO, "[DEBUG] [ISCERTIFIEDVALUE]: " + isCertifiedValue);
    
        return isQuorumHighest && isCertifiedValue;
    }

    private boolean unbound(List<ConsensusMessage> states) {

        // unbound condition: IF S >= N - f and all entries in a quorum of S have a timestamp equal to 0

        for (ConsensusMessage state : states) {
            if (state.getEpoch() != 0) {
                return false;
            }
        }
        return true; 
    }

    // Verifies if the timestamp/value pair (ts, v) has the highest timestamp among a Byzantine quorum of defined entries in S
    private boolean quorumHighest(int ts, Block v, List<ConsensusMessage> messages) {

        int count = 0;

        for (ConsensusMessage message : messages) {

            int messageTs = message.getEpoch();
            Block messageValue = message.getProposedBlock();

            if (messageTs < ts || (messageTs == ts && Objects.equals(messageValue, v))) {
                count++;
            }
        }
        return count >= quorumSize;
    }
    
    // Checks if the value v is certified in the set of messages S
    private boolean certifiedValue(int ts, Block v, List<ConsensusMessage> messages) {
        int count = 0;
        // logger.log(Level.INFO, "[DEBUG] [CERTIFIED_VALUE] [TIMESTAMP]: " + ts);
        // logger.log(Level.INFO, "[DEBUG] [CERTIFIED_VALUE] [VALUE]: " + v);
        // logger.log(Level.INFO, "[DEBUG] [CERTIFIED_VALUE] [MESSAGES]: " + messages);

        for (ConsensusMessage message : messages) {
            // Checks if the writeset contains a pair (ts', v') where ts' ≥ ts and v' = v
            Map<Integer, Block> writeset = message.getBlockWriteset();
            if (writeset != null) { // Null check to avoid NullPointerException
                for (Map.Entry<Integer, Block> entry : writeset.entrySet()) {
                    int entryTs = entry.getKey();
                    Block entryValue = entry.getValue();

                    if (entryTs >= ts && entryValue.equals(v)) {
                        count++;
                        break;
                    }
                }
            }
        }
        // logger.log(Level.INFO, "[DEBUG] [CERTIFIED_VALUE] : Count = " + count + ", Required = " + (f + 1));
        return count > f;
    }


    private void handleWrite(ConsensusMessage message) {
        logger.log(Level.INFO, "[NODE_SERVICE] [HANDLE_WRITE] Handling WRITE message from " + message.getSenderId());

        if (!EncryptionTools.verifySignature(message.toString(), message.getSignature(), nodes.get(message.getSenderId()).getPublicKeyPath())) {
            logger.log(Level.INFO, "⚠️ [NODE_SERVICE] [INVALID_SIGNATURE] WRITE message from " + message.getSenderId());
            triggerTimeout();
            return;
        }

        writeMessageBucket.addMessage(message);
        // Check if there is a valid quorum for WRITE messages
        Optional<Block> quorumValue = writeMessageBucket.hasValidWriteQuorum(message.getEpoch());
        if (quorumValue.isPresent() && !writePhaseDone) {

            writePhaseDone = true;
            logger.log(Level.INFO, "🫅 [DEBUG] Leader Block : " + leaderProposedBlock);
            logger.log(Level.INFO, "🧑 [DEBUG] Current Block : " + currentBlock);

            if (leaderProposedBlock.equals(currentBlock)){

                writeset.put(message.getEpoch(), quorumValue.get());

                ConsensusMessage acceptMessage = new ConsensusMessageBuilder(config.getId(), ConsensusMessage.Type.ACCEPT)
                    .setEpoch(message.getEpoch())
                    .setProposedBlock(currentBlock)
                    .setLeaderId(message.getLeaderId())
                    .build();

                try {
                    String signature = EncryptionTools.sign(acceptMessage.toString(), config.getPrivateKeyPath());
                    acceptMessage.setSignature(signature);
                } catch (Exception e) {
                    logger.log(Level.INFO, "❌ [SIGN_ERROR] Failed to sign ACCEPT message: " + e.getMessage());
                    triggerTimeout();
                    return;
                }

                link.broadcast(acceptMessage);

            }

            triggerTimeout();
            return;
        }
    }

    private void handleAccept(ConsensusMessage message) {
        logger.log(Level.INFO, "[NODE_SERVICE] [HANDLE_ACCEPT] : Handling ACCEPT message from " + message.getSenderId());
        acceptMessageBucket.addMessage(message);

        Optional<Block> quorumValue = acceptMessageBucket.hasValidAcceptQuorum(message.getEpoch());
        if (quorumValue.isPresent() && !valueHasDecided) {
            Block acceptedBlock = quorumValue.get();
            appendToBlockchain(acceptedBlock);
            valueHasDecided = true;

            if (consensusCompletionCallback != null) consensusCompletionCallback.run();

            logger.log(Level.INFO, "[NODE_SERVICE] [LEDGER_INFO] : Accepted Block: " + acceptedBlock);
            logger.log(Level.INFO, "[NODE_SERVICE] [LEDGER_INFO] : Current Blockchain : " + getBlockchain());
            
            LedgerMessage response;

            switch (clientMessageType) {
                case TRANSFER:
                    response = new LedgerMessageBuilder(this.config.getId(), Message.Type.TRANSFER_RESULT)
                        .setLogs(String.join("\n", acceptedBlock.getTransactionLogs()))
                        .setResult("Transfer successful")
                        .setWasOperationSuccessful(true)
                        .setEpoch(currentEpoch)
                        .build();
                    break;

                case SHOW_PROFILE:
                    Account accountData = getBlockchain().getAccount(from);
                    String profile = MessageFormat.format("DepCoin: {0}, ISTCoin: {1}",
                            accountData.getDepCoinBalance().toString(),
                            accountData.getIstCoinBalance().toString());

                    response = new LedgerMessageBuilder(this.config.getId(), Message.Type.SHOW_PROFILE_RESULT)
                        .setLogs(String.join("\n", acceptedBlock.getTransactionLogs()))
                        .setResult(profile)
                        .setWasOperationSuccessful(true)
                        .setEpoch(currentEpoch)
                        .setFrom(clientAddress)
                        .build();
                    break;

                default:
                    logger.log(Level.WARNING, "❌ [NODE_SERVICE] [HANDLE_ACCEPT] Unknown client message type: " + clientMessageType);
                    triggerTimeout();
                    return;
            }

            linkToClients.send(senderID, response);

        }
    }

    private synchronized void appendToBlockchain(Block block) {
        blockchain.addBlock(block, executor, byteArrayOutputStream, contractsBytecode);
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public ProcessConfig getConfig() {
        return config;
    }

    public int getEpoch() {
        return currentEpoch;
    }

    public String getSenderID() {
        return senderID;
    }

    public boolean isConsensusComplete(int epoch) {
        return this.currentEpoch > epoch || this.valueHasDecided;
    }

    private void triggerTimeout() {
        if (!valueHasDecided && consensusCompletionCallback != null) {
            consensusCompletionCallback.run();
        }
    }

    public static void sleep(int seconds) {
        try {
            long milliseconds = seconds * 1000;
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void listen() {
        new Thread(() -> {
            try {
                while (true) {
                    ConsensusMessage message = link.receive();
    
                    switch (config.getBehaviour()) {
                        case DROP:
                            System.out.println("😈 [BEHAVIOUR] drop");
                            continue;
                        case INVALID_SIGNATURE:
                            System.out.println("😈 [BEHAVIOUR] invalid signature");
                            message.setSignature("INVALID");
                            break;
                        case WRONG_BLOCK:
                            System.out.println("😈 [BEHAVIOUR] wrong block");
                            if (message.getType() == ConsensusMessage.Type.STATE ||
                                message.getType() == ConsensusMessage.Type.WRITE ||
                                message.getType() == ConsensusMessage.Type.ACCEPT) {
                                
                                message.setProposedBlock(ByzantineUtils.generateRandomBlock());
                            }
                            break;
                        case DELAY:
                            System.out.println("😈 [BEHAVIOUR] delay");
                            sleep(3);
                            continue;
                        case NONE:
                        default:
                            switch (message.getType()) {
                                case READ -> handleRead(message);
                                case STATE -> handleState(message);
                                case WRITE -> handleWrite(message);
                                case ACCEPT -> handleAccept(message);
                                case ACK -> logger.log(Level.INFO, "[NODE_SERVICE] : Received ACK from " + message.getSenderId());
                                default -> logger.log(Level.INFO, "[NODE_SERVICE] : Received UNKNOWN message type: " + message.getType());
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
