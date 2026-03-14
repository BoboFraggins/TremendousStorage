package net.bobofraggins.intellistore.storage.bulkstorage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes the Bulk Storage Container as a dynamic-slot {@link IItemHandler} for automation.
 *
 * <p>Slot count equals the number of distinct item types currently stored. Each slot presents
 * up to {@code min(maxStackSize, count)} items of its type for extraction. Slot count changes
 * as types are added and removed.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>Damageable items and items with non-default component data are refused.
 *   <li>If the container is full (32,768 total items), the stack is returned unchanged.
 *   <li>Inserts up to remaining pool capacity; remainder is returned.
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>Extracts up to {@code min(amount, maxStackSize, storedCount)} of the type at the slot.
 *   <li>When a type's count reaches zero its slot is removed.
 * </ol>
 */
public class BulkStorageContainerItemHandler implements IItemHandler {

    private final BulkStorageContainerBlockEntity be;

    public BulkStorageContainerItemHandler(BulkStorageContainerBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getSlots() {
        return be.typeCount();
    }

    @Override
    public int getSlotLimit(int slot) {
        return (int) Math.min(Integer.MAX_VALUE, be.getCapacity());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return BulkStorageContainerBlockEntity.accepts(stack);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack type = be.getType(slot);
        if (type.isEmpty()) return ItemStack.EMPTY;
        long count = be.getCount(slot);
        if (count == 0) return ItemStack.EMPTY;
        int visible = (int) Math.min(type.getMaxStackSize(), count);
        return type.copyWithCount(visible);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!BulkStorageContainerBlockEntity.accepts(stack)) return stack;

        long remainder = be.insert(stack, stack.getCount(), simulate);
        if (remainder == stack.getCount()) return stack;
        return remainder == 0 ? ItemStack.EMPTY : stack.copyWithCount((int) remainder);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return be.extract(slot, amount, simulate);
    }
}
