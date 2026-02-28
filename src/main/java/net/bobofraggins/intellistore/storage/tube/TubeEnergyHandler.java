package net.bobofraggins.intellistore.storage.tube;

import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Exposes a tube block entity's connected Network Interface energy buffer as an
 * {@link IEnergyStorage} capability.
 *
 * <p>This allows energy pipes (Pipez, Mekanism cables, etc.) adjacent to any tube in
 * the network to inject energy directly into the NI's buffer, even if they are not
 * physically adjacent to the NI block itself.
 *
 * <p>The handler only supports {@code receiveEnergy}; extraction is not allowed.
 */
public class TubeEnergyHandler implements IEnergyStorage {

    private final TubeBlockEntity tubeBE;

    public TubeEnergyHandler(TubeBlockEntity tubeBE) {
        this.tubeBE = tubeBE;
    }

    /** Resolves the NI from the tube's cached network, or null if unavailable. */
    private NetworkInterfaceBlockEntity resolveNi() {
        NetworkItemHandler network = tubeBE.getNetworkView();
        if (network == null) return null;
        return network.getNetworkInterface();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        NetworkInterfaceBlockEntity ni = resolveNi();
        if (ni == null) return 0;
        return ni.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        NetworkInterfaceBlockEntity ni = resolveNi();
        return ni != null ? ni.getEnergyStored() : 0;
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
