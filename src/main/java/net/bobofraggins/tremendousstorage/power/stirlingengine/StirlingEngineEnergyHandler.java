package net.bobofraggins.tremendousstorage.power.stirlingengine;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Exposes the Stirling Engine's internal energy buffer as an {@link EnergyHandler} capability. */
public class StirlingEngineEnergyHandler implements EnergyHandler {

    private final StirlingEngineBlockEntity be;

    public StirlingEngineEnergyHandler(StirlingEngineBlockEntity be) {
        this.be = be;
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        return 0; // Stirling Engine only produces energy; no external insertion
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        int toExtract = Math.min(amount, be.getEnergyStored());
        if (toExtract > 0) {
            be.extractEnergy(toExtract);
        }
        return toExtract;
    }

    @Override
    public long getAmountAsLong() {
        return be.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return be.getMaxEnergy();
    }
}
