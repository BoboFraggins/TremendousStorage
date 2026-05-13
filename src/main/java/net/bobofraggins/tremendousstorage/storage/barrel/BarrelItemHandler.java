package net.bobofraggins.tremendousstorage.storage.barrel;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes the Barrel's single-type item storage as an {@link IItemHandler} capability.
 *
 * <p>Presents one virtual slot representing the entire stored quantity, capped at the item's
 * max stack size per call. Insert and extract respect the barrel's lock and void-excess settings.
 */
public class BarrelItemHandler implements IItemHandler {

    private final BarrelBlockEntity be;

    public BarrelItemHandler(BarrelBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot != 0 || !be.isLocked() || be.getCount() <= 0) return ItemStack.EMPTY;
        ItemStack type = be.getStoredItem();
        return type.copyWithCount((int) Math.min(be.getCount(), Integer.MAX_VALUE));
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty()) return stack;
        long remainder = be.insert(stack, stack.getCount(), simulate);
        if (remainder <= 0) return ItemStack.EMPTY;
        return stack.copyWithCount((int) Math.min(remainder, Integer.MAX_VALUE));
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
        int capped = Math.min(amount, be.isLocked() ? be.getStoredItem().getMaxStackSize() : 0);
        return be.extract(capped, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return be.isLocked() ? be.getStoredItem().getMaxStackSize() : 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!be.isLocked()) return true;
        return ItemStack.isSameItemSameComponents(be.getStoredItem(), stack);
    }
}
