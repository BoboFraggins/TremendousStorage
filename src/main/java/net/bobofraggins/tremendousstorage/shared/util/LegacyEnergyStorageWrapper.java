package net.bobofraggins.tremendousstorage.shared.util;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Wraps a legacy {@link IEnergyStorage} as an {@link EnergyHandler}.
 *
 * <p>Transaction semantics are NOT supported: operations execute immediately.
 */
@Deprecated
public class LegacyEnergyStorageWrapper implements EnergyHandler {

    private final IEnergyStorage handler;

    public LegacyEnergyStorageWrapper(IEnergyStorage handler) {
        this.handler = handler;
    }

    @Override
    public long getAmountAsLong() {
        return handler.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return handler.getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        return handler.receiveEnergy(amount, false);
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        return handler.extractEnergy(amount, false);
    }
}
