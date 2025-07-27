package tecnico.models;

import java.util.Objects;

public class Instance {
    private int epoch;
    private Block proposedBlock;
    private boolean decided;

    public Instance(int epoch, Block proposedBlock) {
        this.epoch = epoch;
        this.proposedBlock = proposedBlock;
        this.decided = false;
    }

    public int getEpoch() {
        return epoch;
    }

    public Block getProposedBlock() {
        return proposedBlock;
    }

    public boolean isDecided() {
        return decided;
    }

    public void setDecided(boolean decided) {
        this.decided = decided;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instance that = (Instance) o;
        return epoch == that.epoch &&
                Objects.equals(proposedBlock, that.proposedBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(epoch, proposedBlock);
    }
}