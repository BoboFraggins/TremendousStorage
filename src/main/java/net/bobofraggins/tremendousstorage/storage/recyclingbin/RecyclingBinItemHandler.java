package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Item handler for the Recycling Bin. Accepts any item and destroys it,
 * generating 10 mB of Positive Vibes per item destroyed.
 */
public class RecyclingBinItemHandler implements IItemHandler {

    private final RecyclingBinBlockEntity be;

    public RecyclingBinItemHandler(RecyclingBinBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!simulate) {
            be.onItemsDestroyed(stack.getCount());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
