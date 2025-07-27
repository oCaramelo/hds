package tecnico.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import tecnico.communication.ConsensusMessage;
import tecnico.logger.CustomLogger;

public class MessageBucket {
    private final CustomLogger logger;
    private final int quorumSize;
    private final int f;
    private final int nodeCount;
    private final Map<Integer, Map<String, ConsensusMessage>> bucket = new ConcurrentHashMap<>();

    public MessageBucket(int nodeCount) {
        this.logger = new CustomLogger(MessageBucket.class.getName());
        this.nodeCount = nodeCount;
        this.f = (nodeCount - 1) / 3;
        this.quorumSize = (nodeCount + f) / 2 + 1;
    }

    public void addMessage(ConsensusMessage message) {
        int epoch = message.getEpoch();
        bucket.putIfAbsent(epoch, new ConcurrentHashMap<>());
        bucket.get(epoch).put(message.getSenderId(), message);
        // logMessageCounts();
    }

    protected Optional<Block> hasValidQuorum(int epoch, ConsensusMessage.Type type) {
        Map<Block, Integer> frequency = new HashMap<>();

        bucket.getOrDefault(epoch, new ConcurrentHashMap<>()).values().forEach(message -> {
            if (message.getType() == type) {
                Block value = message.getProposedBlock();
                frequency.put(value, frequency.getOrDefault(value, 0) + 1);
            }
        });

        return frequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= quorumSize)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Map<String, ConsensusMessage> getMessages(int epoch) {
        return bucket.getOrDefault(epoch, new ConcurrentHashMap<>());
    }

    private void logMessageCounts() {
        int totalMessages = bucket.values().stream()
                .mapToInt(Map::size)
                .sum();
        logger.info(String.format("Current message counts - Total: %d", totalMessages));
        bucket.forEach((epoch, messages) -> {
            logger.info(String.format("Epoch %d: %d messages", epoch, messages.size()));
        });
    }
}