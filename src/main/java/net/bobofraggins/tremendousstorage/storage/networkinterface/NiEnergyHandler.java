package net.bobofraggins.tremendousstorage.storage.networkinterface;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes the {@link NetworkInterfaceBlockEntity}'s energy buffer as an {@link IEnergyStorage}
 * capability.
 *
 * <p>The NI accepts energy from any adjacent energy pipe (Pipez, Mekanism cables,
 * Stirling Engine push, etc.). It does not allow extraction — the energy buffer is
 * internal-only.
 */
public class NiEnergyHandler implements IEnergyStorage {

    private final NetworkInterfaceBlockEntity be;

    public NiEnergyHandler(NetworkInterfaceBlockEntity be) {
        this.be = be;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return be.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0; // cannot extract from the NI buffer externally
    }

    @Override
    public int getEnergyStored() {
        return be.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return NetworkInterfaceBlockEntity.MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
