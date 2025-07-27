package tecnico.communication.builder;

import tecnico.communication.ConsensusMessage;
import tecnico.communication.Message;
import tecnico.models.Block;

import java.util.Map;

public class ConsensusMessageBuilder {

    private final ConsensusMessage instance;

    public ConsensusMessageBuilder(String sender, Message.Type type) {
        this.instance = new ConsensusMessage(sender, type);
    }

    public ConsensusMessageBuilder setEpoch(int epoch) {
        instance.setEpoch(epoch);
        return this;
    }

    public ConsensusMessageBuilder setLeaderId(String leaderId) {
        instance.setLeaderId(leaderId);
        return this;
    }

    public ConsensusMessageBuilder setProposedBlock(Block block) {
        instance.setProposedBlock(block);
        return this;
    }

    public ConsensusMessageBuilder setWriteset(Map<Integer, Block> writeset) { 
        instance.setBlockWriteset(writeset);
        return this;
    }

    public ConsensusMessageBuilder setMessageId(int messageId) {
        instance.setMessageId(messageId);
        return this;
    }

    public ConsensusMessageBuilder setSignature(String signature) {
        instance.setSignature(signature);
        return this;
    }

    public ConsensusMessage build() {
        return instance;
    }
}