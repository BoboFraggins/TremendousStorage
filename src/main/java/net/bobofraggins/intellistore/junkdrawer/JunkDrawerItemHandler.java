package net.bobofraggins.intellistore.junkdrawer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes the Junk Drawer as a single-slot {@link IItemHandler} for hoppers and automation.
 *
 * <p>The single slot presents up to {@code min(maxStackSize, count)} items for extraction,
 * and accepts any item for insertion (including damageable and NBT items).
 *
 * <p>Insert rules:
 * <ol>
 *   <li>If the drawer is unlocked, it locks to the inserted item type and inserts.
 *   <li>If the drawer is locked to a matching item, inserts up to remaining capacity.
 *   <li>If the drawer is locked to a different item, the stack is returned unchanged.
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>If the drawer is unlocked or count == 0, returns EMPTY.
 *   <li>Extracts up to {@code min(amount, maxStackSize, count)} items.
 *   <li>The drawer stays locked at count 0 after drain.
 * </ol>
 */
public class JunkDrawerItemHandler implements IItemHandler {

    private final JunkDrawerBlockEntity be;

    public JunkDrawerItemHandler(JunkDrawerBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public int getSlotLimit(int slot) {
        return (int) Math.min(Integer.MAX_VALUE, JunkDrawerBlockEntity.CAPACITY);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack stored = be.getStoredType();
        if (stored.isEmpty()) return true; // unlocked — accepts anything
        return ItemStack.isSameItemSameComponents(stored, stack);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack type = be.getStoredType();
        if (type.isEmpty() || be.getCount() == 0) return ItemStack.EMPTY;
        int visible = (int) Math.min(type.getMaxStackSize(), be.getCount());
        return type.copyWithCount(visible);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack stored = be.getStoredType();
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, stack)) {
            return stack; // locked to a different type
        }

        long space = JunkDrawerBlockEntity.CAPACITY - be.getCount();
        if (space <= 0) return stack;

        long toInsert = Math.min(stack.getCount(), space);
        int remainder = (int) (stack.getCount() - toInsert);

        if (!simulate) {
            be.insert(stack, toInsert);
        }

        return remainder == 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        ItemStack type = be.getStoredType();
        if (type.isEmpty() || be.getCount() == 0) return ItemStack.EMPTY;

        long toExtract = Math.min(amount, Math.min(type.getMaxStackSize(), be.getCount()));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            return be.extract(toExtract);
        }

        return type.copyWithCount((int) toExtract);
    }
}
