package tecnico.communication;

import tecnico.models.Block;
import java.util.Map;

public class ConsensusMessage extends Message {

    private String leaderId;
    private Block proposedBlock;
    private Map<Integer, Block> blockWriteset;
    private int consensusEpoch;

    public ConsensusMessage(String senderId, Type type) {
        super(senderId, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConsensusMessage{")
          .append("type=").append(getType())
          .append(", epoch=").append(consensusEpoch)
          .append(", leaderId=").append(leaderId)
          .append(", senderId=").append(getSenderId())
          .append(", messageId=").append(getMessageId())
          .append(", proposedBlock=").append(proposedBlock != null ? proposedBlock.getHash() : "null");

        // if (getType() == Type.STATE && blockWriteset != null) {
        //     sb.append(", blockWriteset=").append(blockWriteset);
        // }

        sb.append('}');
        return sb.toString();
    }


    public int getEpoch() {
        return consensusEpoch;
    }


    public void setEpoch(int epoch) {
        this.consensusEpoch = epoch;
    }

    public boolean isFirstEpoch() {
        return consensusEpoch == 1;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public Block getProposedBlock() {
        return proposedBlock;
    }

    public void setProposedBlock(Block proposedBlock) {
        this.proposedBlock = proposedBlock;
    }

    public Map<Integer, Block> getBlockWriteset() {
        return blockWriteset;
    }

    public void setBlockWriteset(Map<Integer, Block> blockWriteset) {
        this.blockWriteset = blockWriteset;
    }
}