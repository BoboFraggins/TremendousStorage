package net.bobofraggins.tremendousstorage.storage.tank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Tank as a single-tank {@link ResourceHandler}{@code <FluidResource>} for pumps and
 * fluid automation.
 *
 * <p>Properly supports {@link TransactionContext} via a {@link SnapshotJournal} so that
 * simulation passes in {@code ResourceHandlerUtil.moveFirst} don't permanently drain the tank.
 */
public class TankFluidHandler implements ResourceHandler<FluidResource> {

    private final TankBlockEntity be;

    private record TankSnapshot(FluidStack storedFluid, long amount) {}

    private final SnapshotJournal<TankSnapshot> journal = new SnapshotJournal<>() {
        @Override
        protected TankSnapshot createSnapshot() {
            return new TankSnapshot(be.storedFluid.copy(), be.amount);
        }

        @Override
        protected void revertToSnapshot(TankSnapshot snapshot) {
            be.storedFluid = snapshot.storedFluid();
            be.amount = snapshot.amount();
        }

        @Override
        protected void onRootCommit(TankSnapshot originalState) {
            be.notifyFluidChanged();
        }
    };

    public TankFluidHandler(TankBlockEntity be) {
        this.be = be;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        FluidStack stored = be.storedFluid;
        return stored.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stored);
    }

    @Override
    public long getAmountAsLong(int index) {
        return be.amount;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return be.getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        FluidStack stored = be.storedFluid;
        return stored.isEmpty() || resource.matches(stored);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        long space = be.getCapacity() - be.amount;
        int toInsert = be.isVoidExcess() ? amount : (int) Math.min(amount, Math.min(space, Integer.MAX_VALUE));
        if (toInsert <= 0) return 0;

        if (tx != null) {
            journal.updateSnapshots(tx);
        }
        if (be.storedFluid.isEmpty()) {
            be.storedFluid = resource.toStack(1);
        }
        be.amount += toInsert;
        if (tx == null) {
            be.notifyFluidChanged();
        }
        return toInsert;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        FluidStack stored = be.storedFluid;
        if (stored.isEmpty() || !resource.matches(stored)) return 0;
        int toDrain = (int) Math.min(amount, Math.min(be.amount, Integer.MAX_VALUE));
        if (toDrain <= 0) return 0;

        if (tx != null) {
            journal.updateSnapshots(tx);
        }
        be.amount -= toDrain;
        if (tx == null) {
            be.notifyFluidChanged();
        }
        return toDrain;
    }
}
