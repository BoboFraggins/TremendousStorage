package net.bobofraggins.tremendousstorage.shared.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Wraps a legacy {@link IItemHandler} as a {@link ResourceHandler}{@code <ItemResource>}.
 *
 * <p>Transaction semantics are NOT supported: operations execute immediately without
 * rollback support. This is acceptable for vanilla automation (hoppers always commit)
 * but should not be used inside nested transactional contexts.
 */
@Deprecated
public class LegacyItemHandlerWrapper implements ResourceHandler<ItemResource> {

    private final IItemHandler handler;

    public LegacyItemHandlerWrapper(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(handler.getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return handler.isItemValid(index, resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack toInsert = resource.toStack(Math.min(amount, Integer.MAX_VALUE));
        ItemStack remaining = handler.insertItem(index, toInsert, false);
        return amount - remaining.getCount();
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack inSlot = handler.getStackInSlot(index);
        if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, resource.toStack(1))) return 0;
        ItemStack extracted = handler.extractItem(index, Math.min(amount, Integer.MAX_VALUE), false);
        return extracted.getCount();
    }
}
