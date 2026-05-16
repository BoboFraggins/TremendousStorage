package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * {@link IFluidHandlerItem} capability for the Tank block item.
 *
 * <p>Reads and writes fluid state via the {@link TankContents} data component.
 * Capacity is derived from the tier stored in {@code minecraft:block_entity_data}.
 *
 * <p>The handler works on a copy of the item stack; after calling {@link #fill} or
 * {@link #drain(int, FluidAction)} the caller must replace the player's held item with
 * {@link #getContainer()}. {@link net.neoforged.neoforge.fluids.FluidUtil} handles this
 * automatically.
 */
public class TankItemFluidHandler implements IFluidHandlerItem {

    private final ItemStack container;

    public TankItemFluidHandler(ItemStack stack) {
        this.container = stack.copy();
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TankContents contents() {
        TankContents c = container.get(Registration.TANK_CONTENTS.get());
        return c != null ? c : TankContents.EMPTY;
    }

    private long capacityLong() {
        var data = container.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return TankBlockEntity.BASE_CAPACITY;
        StorageTier tier = StorageTier.fromId(data.getUnsafe().getStringOr("Tier", ""));
        return tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // IFluidHandler
    // -------------------------------------------------------------------------

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        TankContents c = contents();
        if (c.storedFluid().isEmpty() || c.amount() == 0) return FluidStack.EMPTY;
        return c.storedFluid().copyWithAmount((int) Math.min(c.amount(), Integer.MAX_VALUE));
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) Math.min(capacityLong(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (stack.isEmpty()) return false;
        TankContents c = contents();
        return c.storedFluid().isEmpty() || FluidStack.isSameFluidSameComponents(c.storedFluid(), stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        TankContents c = contents();
        if (!c.storedFluid().isEmpty() && !FluidStack.isSameFluidSameComponents(c.storedFluid(), resource)) return 0;
        long space = capacityLong() - c.amount();
        int toFill = (int) Math.min(resource.getAmount(), Math.min(space, Integer.MAX_VALUE));
        if (toFill <= 0) return 0;
        if (action.execute()) {
            FluidStack newType = c.storedFluid().isEmpty() ? resource.copyWithAmount(1) : c.storedFluid();
            container.set(
                    Registration.TANK_CONTENTS.get(), new TankContents(newType, c.amount() + toFill, c.bucketMode()));
        }
        return toFill;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        TankContents c = contents();
        if (c.storedFluid().isEmpty() || !FluidStack.isSameFluidSameComponents(c.storedFluid(), resource))
            return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        TankContents c = contents();
        if (c.storedFluid().isEmpty() || c.amount() == 0) return FluidStack.EMPTY;
        int toDrain = (int) Math.min(maxDrain, Math.min(c.amount(), Integer.MAX_VALUE));
        if (toDrain <= 0) return FluidStack.EMPTY;
        FluidStack result = c.storedFluid().copyWithAmount(toDrain);
        if (action.execute()) {
            container.set(
                    Registration.TANK_CONTENTS.get(),
                    new TankContents(c.storedFluid(), c.amount() - toDrain, c.bucketMode()));
        }
        return result;
    }
}
