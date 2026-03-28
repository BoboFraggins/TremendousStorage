package net.bobofraggins.intellistore.power.stirlingengine;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** Exposes the Stirling Engine's internal energy buffer as an {@link IEnergyStorage} capability. */
public class StirlingEngineEnergyHandler implements IEnergyStorage {

    private final StirlingEngineBlockEntity be;

    public StirlingEngineEnergyHandler(StirlingEngineBlockEntity be) {
        this.be = be;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int amount = Math.min(maxExtract, be.getEnergyStored());
        if (!simulate && amount > 0) {
            be.extractEnergy(amount);
        }
        return amount;
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
