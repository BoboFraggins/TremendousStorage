package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Recycling Bin's Positive Vibes fluid tank as a {@link ResourceHandler}{@code
 * <FluidResource>}. Accepts insert and extract of Positive Vibes only; capacity is fixed at
 * {@value RecyclingBinBlockEntity#FLUID_CAPACITY_MB} mB.
 */
public class RecyclingBinFluidHandler implements ResourceHandler<FluidResource> {

    private static final FluidResource VIBES = FluidResource.of(Registration.POSITIVE_VIBES_SOURCE.get());

    private final RecyclingBinBlockEntity be;

    public RecyclingBinFluidHandler(RecyclingBinBlockEntity be) {
        this.be = be;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return be.getVibesAmount() > 0 ? VIBES : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return be.getVibesAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && resource.getFluid() == Registration.POSITIVE_VIBES_SOURCE.get();
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        int space = RecyclingBinBlockEntity.FLUID_CAPACITY_MB - be.getVibesAmount();
        int toFill = Math.min(space, amount);
        if (toFill <= 0) return 0;
        be.addVibes(toFill);
        return toFill;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        return be.extractVibes(amount, false);
    }
}
