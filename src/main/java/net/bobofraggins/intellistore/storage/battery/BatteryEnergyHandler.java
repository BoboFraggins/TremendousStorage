package net.bobofraggins.intellistore.storage.battery;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes the Battery's energy buffer as an {@link IEnergyStorage} capability.
 *
 * <p>Both {@link #canReceive()} and {@link #canExtract()} are supported, making the battery a
 * true buffer — other machines can push surplus FE in and pull from it when they need power.
 */
public class BatteryEnergyHandler implements IEnergyStorage {

    private final BatteryBlockEntity be;

    public BatteryEnergyHandler(BatteryBlockEntity be) {
        this.be = be;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return be.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return be.extractEnergy(maxExtract, simulate);
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
        return be.getEnergyStored() > 0;
    }

    @Override
    public boolean canReceive() {
        return be.getEnergyStored() < be.getMaxEnergy();
    }
}
