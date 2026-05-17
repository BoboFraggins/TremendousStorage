package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Item handler for the Recycling Bin. Accepts any item and destroys it,
 * generating 10 mB of Positive Vibes per item destroyed.
 *
 * <p>Empty fluid containers are routed to the fluid-fill slot (slot 0 of the transfer container)
 * when that slot is available; otherwise they are voided like any other item.
 */
public class RecyclingBinItemHandler implements ResourceHandler<ItemResource> {

    private final RecyclingBinBlockEntity be;

    public RecyclingBinItemHandler(RecyclingBinBlockEntity be) {
        this.be = be;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return 64;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (index != 0 || resource.isEmpty() || amount <= 0) return 0;
        ItemStack stack = resource.toStack(1);
        if (RecyclingBinMenu.isEmptyFluidContainer(stack)) {
            if (be.transferContainer.getItem(0).isEmpty()) {
                be.transferContainer.setItem(0, stack);
                int destroyed = amount - 1;
                if (destroyed > 0) be.onItemsDestroyed(destroyed);
                return amount;
            }
        }
        be.onItemsDestroyed(amount);
        return amount;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        return 0;
    }
}
