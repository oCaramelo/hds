package tecnico;

import tecnico.communication.LedgerMessage;
import tecnico.communication.Message;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class ClientMessageBucket {
    private final int requiredMessages; // f + 1
    private final ConcurrentSkipListMap<Integer, Map<String, LedgerMessage>> bucket = new ConcurrentSkipListMap<>();

    public ClientMessageBucket(int nodeCount) {
        int f = (nodeCount - 1) / 3;
        this.requiredMessages = f + 1;
    }

    public void addMessage(LedgerMessage message) {
        bucket.computeIfAbsent(message.getEpoch(), 
            k -> new ConcurrentHashMap<>()).put(message.getSenderId(), message);
    }

    public Optional<LedgerMessage> hasSufficientSuccessfulResultMessage(int epoch, Message.Type type) {
        if (!bucket.containsKey(epoch)) {
            return Optional.empty();
        }

        List<LedgerMessage> successfulMessages = bucket.get(epoch).values().stream()
            .filter(m -> m.getType() == type && m.wasOperationSuccessful())
            .collect(Collectors.toList());

        if (successfulMessages.size() >= requiredMessages) {
            return Optional.of(successfulMessages.get(0));
        }

        return Optional.empty();
    }


    public Map<String, LedgerMessage> getMessages(int epoch) {
        return bucket.getOrDefault(epoch, Collections.emptyMap());
    }

    public void clearEpoch(int epoch) {
        bucket.remove(epoch);
    }

    public void clearEpochsBelow(int epoch) {
        bucket.headMap(epoch).clear();
    }

    public int getRequiredMessages() {
        return requiredMessages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClientMessageBucket{requiredMessages=").append(requiredMessages).append(", bucket=\n");

        bucket.forEach((epoch, messages) -> {
            sb.append("  Epoch ").append(epoch).append(":\n");
            messages.forEach((sender, msg) -> {
                sb.append("    Sender ").append(sender).append(": ").append(msg).append("\n");
            });
        });

        sb.append("}");
        return sb.toString();
    }
}