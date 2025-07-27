package tecnico.services;

import tecnico.communication.AuthenticatedPerfectLink;
import tecnico.communication.LedgerMessage;
import tecnico.communication.Message;
import tecnico.logger.CustomLogger;
import tecnico.communication.builder.LedgerMessageBuilder;
import tecnico.models.Account;
import tecnico.models.Block;
import tecnico.models.Transaction;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

public class LedgerService implements UDPService {

    private final NodeService nodeService;
    private final CustomLogger logger;
    private final AuthenticatedPerfectLink<LedgerMessage> authenticatedPerfectLink;
    private static final long CONSENSUS_TIMEOUT = 2000;

    public LedgerService(
            AuthenticatedPerfectLink<LedgerMessage> authenticatedPerfectLink,
            NodeService nodeService
    ) {
        this.nodeService = nodeService;
        this.authenticatedPerfectLink = authenticatedPerfectLink;
        this.logger = new CustomLogger(LedgerService.class.getName());
    }

    public void uponTransfer(LedgerMessage message) {
        String from = message.getFrom();
        String to = message.getTo();
        BigInteger value = message.getValue();
        String inputData = message.getInputData();
        String coinType = message.getCoinType();

        if (from == null || to == null || value == null || (inputData == null && coinType == null)) {
            logger.error("[LEDGER_SERVICE] [ERROR] Received TRANSFER request with missing fields");
            LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.TRANSFER_RESULT)
                .setResult("Error: Missing transfer fields")
                .setEpoch(nodeService.getEpoch())
                .build();
            authenticatedPerfectLink.send(message.getSenderId(), response);
            return;
        }

        if (coinType.equals("Depcoin")){
            BigInteger fromAccountBalance = nodeService.getBlockchain().getAccount(from).getDepCoinBalance();
        
            if (fromAccountBalance.compareTo(value) < 0) {
                logger.error(MessageFormat.format("[LEDGER_SERVICE] [ERROR] Insufficient DepCoin balance for transfer from {0} to {1}", from, to));
                LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.TRANSFER_RESULT)
                    .setResult("Error: Insufficient DepCoin balance")
                    .setEpoch(nodeService.getEpoch())
                    .build();
                authenticatedPerfectLink.send(message.getSenderId(), response);
                return;
            }
        } else if (coinType.equals("ISTCoin")) {
            BigInteger fromAccountBalance = nodeService.getBlockchain().getAccount(from).getIstCoinBalance();

            if (fromAccountBalance.compareTo(value) < 0) {
                logger.error(MessageFormat.format("[LEDGER_SERVICE] [ERROR] Insufficient ISTCoin balance for transfer from {0} to {1}", from, to));
                LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.TRANSFER_RESULT)
                    .setResult("Error: Insufficient ISTCoin balance")
                    .setEpoch(nodeService.getEpoch())
                    .build();
                authenticatedPerfectLink.send(message.getSenderId(), response);
                return;
            }
        }
        

        logger.info(MessageFormat.format("[LEDGER_SERVICE] Received TRANSFER request from client {0}: from={1}, to={2}, value={3}, type={4}",
                message.getSenderId(), from, to, value, inputData == null || inputData.isEmpty() ? "DepCoin" : "ISTCoin"));

        // Create Transaction
        Transaction transaction = new Transaction(from, to, value, inputData, coinType, "Transfer");

        // Create Block
        List<Transaction> transactions = List.of(transaction);
        Block block = new Block(transactions);

        final String clientId = message.getSenderId();
        final int currentEpoch = nodeService.getEpoch();

        Timer timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!nodeService.isConsensusComplete(currentEpoch)) {
                    logger.warn(MessageFormat.format("[LEDGER_SERVICE] [TIMEOUT] Consensus for epoch {0} timed out", currentEpoch));
                    
                    LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.TRANSFER_RESULT)
                        .setResult("Error: Consensus timed out")
                        .setEpoch(currentEpoch)
                        .setWasOperationSuccessful(false)
                        .build();
                    authenticatedPerfectLink.send(clientId, response);
                }
            }
        }, CONSENSUS_TIMEOUT);

        nodeService.startConsensus(message, block, () -> {
            timeoutTimer.cancel();

            // LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.TRANSFER_RESULT)
            //             .setResult("SUCCESS")
            //             .setEpoch(currentEpoch)
            //             .build();
            
            // authenticatedPerfectLink.send(clientId, response);
        });
    }

    public void uponShowProfile(LedgerMessage message) {
        String from = message.getFrom();
        String inputData = message.getInputData();
        if (from == null) {
            logger.error("[LEDGER_SERVICE] [ERROR] Received SHOW_PROFILE request with null account");

            LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.SHOW_PROFILE_RESULT)
                    .setResult("Error: Missing account")
                    .setEpoch(nodeService.getEpoch())
                    .setWasOperationSuccessful(false)
                    .build();

            authenticatedPerfectLink.send(message.getSenderId(), response);
            return;
        }

        logger.info(MessageFormat.format("[LEDGER_SERVICE] Received SHOW_PROFILE request for account {0} from client {1}",
                from, message.getSenderId()));

        Transaction depCoinBalanceTx = new Transaction(from, "", "DepCoin", "Balance");

        Transaction istCoinBalanceTx = new Transaction(from, inputData, "ISTCoin", "Balance");

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(depCoinBalanceTx);
        transactions.add(istCoinBalanceTx);
        
        Block block = new Block(transactions);

        final String clientId = message.getSenderId();
        final int currentEpoch = nodeService.getEpoch();

        Timer timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!nodeService.isConsensusComplete(currentEpoch)) {
                    logger.warn(MessageFormat.format("[LEDGER_SERVICE] [TIMEOUT] Consensus for epoch {0} timed out", currentEpoch));
                    
                    LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.SHOW_PROFILE_RESULT)
                        .setResult("Error: Consensus timed out")
                        .setEpoch(currentEpoch)
                        .build();
                    authenticatedPerfectLink.send(clientId, response);
                }
            }
        }, CONSENSUS_TIMEOUT);

        nodeService.startConsensus(message, block, () -> {
            timeoutTimer.cancel();

            // LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.SHOW_PROFILE_RESULT)
            //             .setResult("SUCCESS")
            //             .setEpoch(currentEpoch)
            //             .build();
            
            // authenticatedPerfectLink.send(clientId, response);
        });
    }

    public void uponShowBlockchain(LedgerMessage message) {
        logger.info(MessageFormat.format("[LEDGER_SERVICE] Received SHOW_BLOCKCHAIN request from client {0}", message.getSenderId()));

        List<Block> blockchain = nodeService.getBlockchain().getBlocks();
        String blockchainState = blockchain.isEmpty() ? "[]" : blockchain.stream()
                .map(Block::toString)
                .collect(Collectors.joining(",\n", "[", "]"));

        final String clientId = message.getSenderId();
        final int currentEpoch = nodeService.getEpoch();

        LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.SHOW_BLOCKCHAIN_RESULT)
                .setResult(blockchainState)
                .setWasOperationSuccessful(true)
                .setEpoch(currentEpoch)
                .build();

        authenticatedPerfectLink.send(clientId, response);
    }

    public void uponShowNetwork(LedgerMessage message) {
        logger.info(MessageFormat.format("[LEDGER_SERVICE] Received SHOW_NETWORK request from client {0}", message.getSenderId()));

        Map<String, Account> accounts = nodeService.getBlockchain().getAccounts();

        final String clientId = message.getSenderId();
        final int currentEpoch = nodeService.getEpoch();

        Set<String> addresses = accounts.keySet();

        String accountList = addresses.isEmpty() ? "": addresses.stream()
                .collect(Collectors.joining(",\n", "[", "]"));

        LedgerMessage response = new LedgerMessageBuilder(nodeService.getConfig().getId(), Message.Type.SHOW_NETWORK_RESULT)
                .setResult(accountList)
                .setWasOperationSuccessful(true)
                .setFrom(message.getFrom())
                .setEpoch(currentEpoch)
                .build();

        authenticatedPerfectLink.send(clientId, response);
    }


    @Override
    public void listen() {
        logger.info("[LISTENING]");

        new Thread(() -> {
            while (true) {
                try {
                    LedgerMessage message = authenticatedPerfectLink.receive();
                    new Thread(() -> {
                        switch (message.getType()) {
                            case TRANSFER -> uponTransfer(message);
                            case SHOW_PROFILE -> uponShowProfile(message);
                            case SHOW_BLOCKCHAIN -> uponShowBlockchain(message);
                            case SHOW_NETWORK -> uponShowNetwork(message);
                            case ACK -> logger.info(MessageFormat.format("[LEDGER_SERVICE] Received ACK message from {0}", message.getSenderId()));
                            case IGNORE -> {
                                // Do nothing for IGNORE messages
                            }
                            default ->
                                    logger.warn(MessageFormat.format("[LEDGER_SERVICE] Received unknown message type: {0} from {1}",
                                            message.getType(), message.getSenderId()));
                        }
                    }).start();
                } catch (Exception e) {
                    logger.error(MessageFormat.format("[LEDGER_SERVICE] [ERROR] Error receiving message: {0}", e.getMessage()));
                }
            }
        }).start();
    }
}