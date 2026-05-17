package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * {@link ResourceHandler}{@code <FluidResource>} capability for the Tank block item.
 *
 * <p>Reads and writes fluid state via the {@link TankContents} data component. Capacity is derived
 * from the tier stored in {@code minecraft:block_entity_data}.
 *
 * <p>The handler works on a copy of the item stack. After calling {@link #insert} or
 * {@link #extract} the caller can retrieve the updated stack via {@link #getContainer()}.
 */
public class TankItemFluidHandler implements ResourceHandler<FluidResource> {

    private final ItemStack container;

    public TankItemFluidHandler(ItemStack stack) {
        this.container = stack.copy();
    }

    public ItemStack getContainer() {
        return container;
    }

    private TankContents contents() {
        TankContents c = container.get(Registration.TANK_CONTENTS.get());
        return c != null ? c : TankContents.EMPTY;
    }

    private long capacityLong() {
        var data = container.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return TankBlockEntity.BASE_CAPACITY;
        StorageTier tier = StorageTier.fromId(data.copyTagWithoutId().getStringOr("Tier", ""));
        return tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        TankContents c = contents();
        return (c.storedFluid().isEmpty() || c.amount() == 0) ? FluidResource.EMPTY : FluidResource.of(c.storedFluid());
    }

    @Override
    public long getAmountAsLong(int index) {
        return contents().amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return capacityLong();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        TankContents c = contents();
        return c.storedFluid().isEmpty() || resource.matches(c.storedFluid());
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (!isValid(index, resource) || amount <= 0) return 0;
        TankContents c = contents();
        long space = capacityLong() - c.amount();
        int toFill = (int) Math.min(amount, Math.min(space, Integer.MAX_VALUE));
        if (toFill <= 0) return 0;
        FluidStack newType = c.storedFluid().isEmpty() ? resource.toStack(1) : c.storedFluid();
        container.set(Registration.TANK_CONTENTS.get(), new TankContents(newType, c.amount() + toFill, c.bucketMode()));
        return toFill;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        TankContents c = contents();
        if (c.storedFluid().isEmpty() || !resource.matches(c.storedFluid())) return FluidStack.EMPTY.getAmount();
        int toDrain = (int) Math.min(amount, Math.min(c.amount(), Integer.MAX_VALUE));
        if (toDrain <= 0) return 0;
        container.set(
                Registration.TANK_CONTENTS.get(),
                new TankContents(c.storedFluid(), c.amount() - toDrain, c.bucketMode()));
        return toDrain;
    }
}
