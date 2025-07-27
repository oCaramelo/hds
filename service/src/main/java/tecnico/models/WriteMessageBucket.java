package tecnico.models;

import java.util.Optional;
import tecnico.communication.ConsensusMessage;

public class WriteMessageBucket extends MessageBucket {
    public WriteMessageBucket(int nodeCount) {
        super(nodeCount);
    }

    public Optional<Block> hasValidWriteQuorum(int epoch) {
        return hasValidQuorum(epoch, ConsensusMessage.Type.WRITE);
    }
}