package net.bobofraggins.intellistore.power.stirlingengine;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes {@link StirlingEngineBlockEntity}'s internal buffer as an {@link IEnergyStorage}
 * capability. The Stirling Engine only sends energy; it does not accept energy from outside
 * (energy injection happens via {@link StirlingEngineBlockEntity#receiveEnergy} only
 * for the tube-network side, not from arbitrary pipes).
 *
 * <p>External energy pipes (Pipez, Mekanism cables, etc.) query this capability and can
 * extract energy from the generator.
 */
public class StirlingEngineEnergyHandler implements IEnergyStorage {

    private final StirlingEngineBlockEntity be;

    public StirlingEngineEnergyHandler(StirlingEngineBlockEntity be) {
        this.be = be;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // The Stirling Engine does not accept energy from external sources
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int stored = be.getEnergyStored();
        int toExtract = Math.min(maxExtract, stored);
        if (!simulate && toExtract > 0) {
            // Access the stored energy field via the package-private receiveEnergy trick:
            // We subtract by "receiving" a negative amount is not standard, so we use a direct path.
            // Since this handler is in the same package, we call the package-visible helper.
            be.extractEnergy(toExtract);
        }
        return toExtract;
    }

    @Override
    public int getEnergyStored() {
        return be.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return be.getMaxEnergy();
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
