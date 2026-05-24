package net.bobofraggins.tremendousstorage.storage.chest;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.IPreferredStorage;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Tremendous Chest as a dynamic-slot {@link ResourceHandler}{@code <ItemResource>} for
 * automation.
 *
 * <p>Slot count equals the number of distinct item types currently stored. Each slot presents
 * up to {@code min(maxStackSize, count)} items of its type for extraction. Slot count changes
 * as types are added and removed.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>Damageable items and items with non-default component data are refused.
 *   <li>If the container is full (32,768 total items), nothing is inserted.
 *   <li>Inserts up to remaining pool capacity; returns amount inserted.
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>Extracts up to {@code min(amount, maxStackSize, storedCount)} of the type at the slot.
 *   <li>When a type's count reaches zero its slot is removed.
 * </ol>
 */
public class ChestItemHandler implements ResourceHandler<ItemResource>, IKeyCounterContributor, IPreferredStorage {

    private final ChestBlockEntity be;

    public ChestItemHandler(ChestBlockEntity be) {
        this.be = be;
    }

    @Override
    public int size() {
        // Always expose at least one slot so hoppers can insert into an empty chest.
        return Math.max(1, be.typeCount());
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return be.getCapacity();
    }

    @Override
    public long getItemCapacity() {
        return be.getCapacity();
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return be.acceptsItem(resource.toStack(1));
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack type = be.getType(index);
        if (type.isEmpty()) return ItemResource.EMPTY;
        long count = be.getCount(index);
        if (count == 0) return ItemResource.EMPTY;
        return ItemResource.of(type);
    }

    @Override
    public long getAmountAsLong(int index) {
        ItemStack type = be.getType(index);
        if (type.isEmpty()) return 0;
        return be.getCount(index);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack stack = resource.toStack(amount);
        if (!be.acceptsItem(stack)) return 0;
        long remainder = be.insert(stack, amount, false);
        return amount - (int) Math.min(remainder, amount);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack typeInSlot = be.getType(index);
        if (typeInSlot.isEmpty()) return 0;
        if (!ItemStack.isSameItemSameComponents(typeInSlot, resource.toStack(1))) return 0;
        ItemStack extracted = be.extract(index, amount, false);
        return extracted.getCount();
    }

    @Override
    public void contributeToKeyCounter(KeyCounter kc) {
        be.populateKeyCounter(kc);
    }

    @Override
    public boolean isPreferredFor(StorageKey key) {
        return be.contains(key);
    }
}
