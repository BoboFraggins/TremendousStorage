package net.bobofraggins.tremendousstorage.storage.tube;

import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a tube block entity's connected Network Interface energy buffer as an
 * {@link EnergyHandler} capability.
 *
 * <p>This allows energy pipes (Pipez, Mekanism cables, etc.) adjacent to any tube in
 * the network to inject energy directly into the NI's buffer, even if they are not
 * physically adjacent to the NI block itself.
 *
 * <p>The handler supports {@code insert} for external energy injection, and
 * {@code extract} for pulling <em>spare</em> energy — the amount above the NI's
 * current-tick consumption reserve — allowing energy machines to draw from the network.
 */
public class TubeEnergyHandler implements EnergyHandler {

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
    public int insert(int amount, TransactionContext tx) {
        NetworkInterfaceBlockEntity ni = resolveNi();
        if (ni == null) return 0;
        return ni.receiveEnergy(amount, false);
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        NetworkInterfaceBlockEntity ni = resolveNi();
        if (ni == null) return 0;
        int spare = ni.getEnergyStored() - ni.getTotalConsumption();
        if (spare <= 0) return 0;
        return ni.extractEnergyForDistribution(Math.min(amount, spare), false);
    }

    @Override
    public long getAmountAsLong() {
        NetworkInterfaceBlockEntity ni = resolveNi();
        return ni != null ? ni.getEnergyStored() : 0;
    }

    @Override
    public long getCapacityAsLong() {
        return NetworkInterfaceBlockEntity.MAX_ENERGY;
    }
}
