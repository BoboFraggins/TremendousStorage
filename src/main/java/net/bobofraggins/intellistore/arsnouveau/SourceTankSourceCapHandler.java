package net.bobofraggins.intellistore.arsnouveau;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;

/**
 * Exposes the Source Tank as an {@link ISourceCap} for Ars Nouveau's source network.
 *
 * <p>This allows Ars Nouveau source relays, obelisks, and other source-transferring blocks to
 * push source into or pull source from the Source Tank automatically.
 */
public class SourceTankSourceCapHandler implements ISourceCap {

    private final SourceTankBlockEntity be;

    public SourceTankSourceCapHandler(SourceTankBlockEntity be) {
        this.be = be;
    }

    @Override
    public boolean canAcceptSource(int amount) {
        return be.isVoidExcess() || be.getAmount() < SourceTankBlockEntity.CAPACITY;
    }

    @Override
    public boolean canProvideSource(int amount) {
        return be.getAmount() >= amount;
    }

    @Override
    public int getMaxExtract() {
        return SourceTankBlockEntity.CAPACITY;
    }

    @Override
    public int getMaxReceive() {
        return SourceTankBlockEntity.CAPACITY;
    }

    @Override
    public int getSource() {
        return be.getAmount();
    }

    @Override
    public int getSourceCapacity() {
        return SourceTankBlockEntity.CAPACITY;
    }

    @Override
    public void setSource(int source) {
        // Direct set: extract current then receive the new amount
        int current = be.getAmount();
        if (source < current) {
            be.extract(current - source, false);
        } else if (source > current) {
            be.receive(source - current, false);
        }
    }

    @Override
    public void setMaxSource(int max) {
        // Capacity is fixed — ignore
    }

    @Override
    public int receiveSource(int maxReceive, boolean simulate) {
        if (be.isVoidExcess()) {
            // Accept all source; only store up to remaining capacity
            be.receive(maxReceive, simulate);
            return maxReceive;
        }
        return be.receive(maxReceive, simulate);
    }

    @Override
    public int extractSource(int maxExtract, boolean simulate) {
        return be.extract(maxExtract, simulate);
    }
}
