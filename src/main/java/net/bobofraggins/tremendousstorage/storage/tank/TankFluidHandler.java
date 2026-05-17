package net.bobofraggins.tremendousstorage.storage.tank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Tank as a single-tank {@link ResourceHandler}{@code <FluidResource>} for pumps and
 * fluid automation.
 *
 * <p>Insert: locks to fluid type if unlocked, fills up to capacity, rejects mismatches.
 * Extract: drains up to requested amount from stored fluid.
 */
public class TankFluidHandler implements ResourceHandler<FluidResource> {

    private final TankBlockEntity be;

    public TankFluidHandler(TankBlockEntity be) {
        this.be = be;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        FluidStack stored = be.getStoredFluid();
        return stored.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stored);
    }

    @Override
    public long getAmountAsLong(int index) {
        return be.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return be.getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        FluidStack stored = be.getStoredFluid();
        return stored.isEmpty() || resource.matches(stored);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        if (be.isVoidExcess()) {
            be.insert(resource.toStack(amount), amount, false);
            return amount;
        }
        long inserted = be.insert(resource.toStack(amount), amount, false);
        return (int) inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        FluidStack stored = be.getStoredFluid();
        if (stored.isEmpty() || !resource.matches(stored)) return 0;
        FluidStack extracted = be.extract(amount, false);
        return extracted.getAmount();
    }
}
