package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes an {@link IFluidHandler}'s contents as a virtual {@link IItemHandler} slot containing
 * filled buckets, so the SAT can display and extract fluid as bucket-form items.
 *
 * <p>The adapter presents at most one slot. The slot is absent ({@link #getSlots()} == 0) when
 * the handler is empty. When present, the stack is the appropriate filled bucket with count == 1.
 *
 * <p>Insertion is always refused — fluids are inserted via the normal {@code IFluidHandler}
 * capability, not through the SAT item path.
 */
public class TankItemAdapter implements IItemHandler, IKeyCounterContributor {

    private final IFluidHandler fluidHandler;

    public TankItemAdapter(IFluidHandler fluidHandler) {
        this.fluidHandler = fluidHandler;
    }

    // -------------------------------------------------------------------------
    // IItemHandler
    // -------------------------------------------------------------------------

    @Override
    public int getSlots() {
        FluidStack fluid = fluidHandler.getFluidInTank(0);
        if (fluid.isEmpty()) return 0;
        Item bucket = fluid.getFluid().getBucket();
        return bucket == Items.AIR ? 0 : 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        FluidStack fluid = fluidHandler.getFluidInTank(0);
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        Item bucketItem = fluid.getFluid().getBucket();
        if (bucketItem == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(bucketItem);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack; // insertion via SAT is not supported for fluid tanks
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0) return ItemStack.EMPTY;
        FluidStack fluid = fluidHandler.getFluidInTank(0);
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        long buckets = fluid.getAmount() / 1000;
        if (buckets == 0) return ItemStack.EMPTY;

        int toExtract = (int) Math.min(amount, buckets);
        long mbToExtract = (long) toExtract * 1000;

        Item bucketItem = fluid.getFluid().getBucket();
        FluidStack drained = fluidHandler.drain(
                (int) mbToExtract, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        int bucketsExtracted = drained.isEmpty() ? 0 : drained.getAmount() / 1000;
        if (bucketsExtracted == 0) return ItemStack.EMPTY;

        return new ItemStack(bucketItem, bucketsExtracted);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE; // virtual slot — actual limit is tank capacity
    }

    // -------------------------------------------------------------------------
    // IKeyCounterContributor
    // -------------------------------------------------------------------------

    @Override
    public void contributeToKeyCounter(KeyCounter kc) {
        FluidStack fluid = fluidHandler.getFluidInTank(0);
        if (fluid.isEmpty()) return;
        Item bucketItem = fluid.getFluid().getBucket();
        if (bucketItem == Items.AIR) return;
        kc.add(StorageKey.of(new ItemStack(bucketItem)), fluid.getAmount());
    }
}
