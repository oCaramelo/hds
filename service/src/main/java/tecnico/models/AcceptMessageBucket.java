package tecnico.models;

import java.util.Optional;
import tecnico.communication.ConsensusMessage;

public class AcceptMessageBucket extends MessageBucket {
    public AcceptMessageBucket(int nodeCount) {
        super(nodeCount);
    }

    public Optional<Block> hasValidAcceptQuorum(int epoch) {
        return hasValidQuorum(epoch, ConsensusMessage.Type.ACCEPT);
    }
}