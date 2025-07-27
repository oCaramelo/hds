package tecnico.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import tecnico.communication.ConsensusMessage;

public class StateMessageBucket extends MessageBucket {
    private final Map<Integer, List<ConsensusMessage>> stateMessages = new ConcurrentHashMap<>();

    public StateMessageBucket(int nodeCount) {
        super(nodeCount);
    }

    @Override
    public void addMessage(ConsensusMessage message) {
        if (message.getType() == ConsensusMessage.Type.STATE) {
            int epoch = message.getEpoch();
            stateMessages.putIfAbsent(epoch, new ArrayList<>());
            stateMessages.get(epoch).add(message);
        }
        super.addMessage(message);
    }

    public Optional<Block> hasValidStateQuorum(int epoch) {
        return hasValidQuorum(epoch, ConsensusMessage.Type.STATE);
    }

    public List<ConsensusMessage> getStates(int epoch) {
        return stateMessages.getOrDefault(epoch, new ArrayList<>());
    }
}