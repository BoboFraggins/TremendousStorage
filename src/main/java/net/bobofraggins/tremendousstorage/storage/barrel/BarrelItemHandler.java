package net.bobofraggins.tremendousstorage.storage.barrel;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Barrel's single-type item storage as a {@link ResourceHandler}{@code <ItemResource>}
 * capability.
 *
 * <p>Presents one virtual slot representing the entire stored quantity, capped at the item's
 * max stack size per call. Insert and extract respect the barrel's lock and void-excess settings.
 */
public class BarrelItemHandler implements ResourceHandler<ItemResource> {

    private final BarrelBlockEntity be;

    public BarrelItemHandler(BarrelBlockEntity be) {
        this.be = be;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        if (index != 0 || !be.isLocked() || be.getCount() <= 0) return ItemResource.EMPTY;
        return ItemResource.of(be.getStoredItem());
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index != 0 || !be.isLocked()) return 0;
        return be.getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (index != 0) return 0;
        return be.isLocked() ? be.getStoredItem().getMaxStackSize() : 64;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (index != 0 || resource.isEmpty()) return false;
        if (!be.isLocked()) return true;
        return ItemStack.isSameItemSameComponents(be.getStoredItem(), resource.toStack(1));
    }

    /**
     * Returns how many of {@code amount} items would actually end up stored in the barrel
     * (i.e. respecting physical capacity, ignoring the void-excess "claim all" shortcut).
     *
     * <p>Used by {@link net.bobofraggins.tremendousstorage.storage.networkinterface.NiItemHandler}
     * so that a full void-excess barrel does not swallow a network insert that could otherwise
     * flow to a lower-priority storage with real space.
     */
    public int getHonestInsertableAmount(int amount) {
        if (!be.isVoidExcess()) return amount;
        long space = be.getCapacity() - be.getCount();
        return (int) Math.min(amount, Math.max(0L, space));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (index != 0 || resource.isEmpty() || amount <= 0) return 0;
        long remainder = be.insert(resource.toStack(amount), amount, false);
        return amount - (int) Math.min(remainder, amount);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (index != 0 || resource.isEmpty() || amount <= 0) return 0;
        if (!be.isLocked() || be.getCount() <= 0) return 0;
        if (!ItemStack.isSameItemSameComponents(be.getStoredItem(), resource.toStack(1))) return 0;
        int capped = (int) Math.min(amount, be.getStoredItem().getMaxStackSize());
        ItemStack extracted = be.extract(capped, false);
        return extracted.getCount();
    }
}
