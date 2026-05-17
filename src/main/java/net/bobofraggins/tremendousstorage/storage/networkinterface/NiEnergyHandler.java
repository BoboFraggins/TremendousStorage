package net.bobofraggins.tremendousstorage.storage.networkinterface;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the {@link NetworkInterfaceBlockEntity}'s energy buffer as an {@link EnergyHandler}
 * capability.
 *
 * <p>The NI accepts energy from any adjacent energy pipe (Pipez, Mekanism cables,
 * Stirling Engine push, etc.). It does not allow extraction — the energy buffer is
 * internal-only.
 */
public class NiEnergyHandler implements EnergyHandler {

    private final NetworkInterfaceBlockEntity be;

    public NiEnergyHandler(NetworkInterfaceBlockEntity be) {
        this.be = be;
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        return be.receiveEnergy(amount, false);
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        return 0; // cannot extract from the NI buffer externally
    }

    @Override
    public long getAmountAsLong() {
        return be.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return NetworkInterfaceBlockEntity.MAX_ENERGY;
    }
}
