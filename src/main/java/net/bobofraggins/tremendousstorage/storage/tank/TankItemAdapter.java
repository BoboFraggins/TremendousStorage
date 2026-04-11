package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes a {@link TankBlockEntity}'s contents as a virtual {@link IItemHandler} slot
 * containing filled buckets, so the SAT can display and extract fluid as bucket-form items.
 *
 * <p>The adapter presents at most one slot. The slot is absent ({@link #getSlots()} == 0) when
 * the tank holds less than 1 000 mB. When present, the stack is the appropriate filled bucket
 * with count == min(bucketCount, bucketItem.maxStackSize).
 *
 * <p>Insertion is always refused — fluids are inserted via the normal {@code IFluidHandler}
 * capability, not through the SAT item path.
 */
public class TankItemAdapter implements IItemHandler, IKeyCounterContributor {

    private final TankBlockEntity tank;

    public TankItemAdapter(TankBlockEntity tank) {
        this.tank = tank;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private long bucketCount() {
        return tank.getAmount() / 1000;
    }

    // -------------------------------------------------------------------------
    // IItemHandler
    // -------------------------------------------------------------------------

    @Override
    public int getSlots() {
        return bucketCount() > 0 ? 1 : 0;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        long count = bucketCount();
        if (count == 0) return ItemStack.EMPTY;
        Item bucketItem = tank.getStoredFluid().getFluid().getBucket();
        ItemStack prototype = new ItemStack(bucketItem);
        int displayCount = (int) Math.min(count, prototype.getMaxStackSize());
        return prototype.copyWithCount(displayCount);
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
        long buckets = bucketCount();
        if (buckets == 0) return ItemStack.EMPTY;

        int toExtract = (int) Math.min(amount, buckets);
        long mbToExtract = (long) toExtract * 1000;

        // Capture fluid type before extraction (tank stays locked even at 0 mB)
        FluidStack storedBefore = tank.getStoredFluid();
        FluidStack drained = tank.extract(mbToExtract, simulate);
        int bucketsExtracted = drained.isEmpty() ? 0 : drained.getAmount() / 1000;
        if (bucketsExtracted == 0) return ItemStack.EMPTY;

        Item bucketItem = storedBefore.getFluid().getBucket();
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
        long count = bucketCount();
        if (count == 0) return;
        Item bucketItem = tank.getStoredFluid().getFluid().getBucket();
        kc.add(StorageKey.of(new ItemStack(bucketItem)), count);
    }
}
