package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Three-slot {@link ResourceHandler}{@code <ItemResource>} for compacting barrels.
 *
 * <p>Slots 0–2 map to ResourceHandler slots 0–2. {@code baseSlot} indicates which logical slot
 * holds the real stored item; slots below it are always empty and reject all operations. Slots
 * at and above {@code baseSlot} expose compressed tiers.
 *
 * <p>Examples (baseSlot=0): slot 0 = nuggets, slot 1 = ingots, slot 2 = blocks.
 * (baseSlot=1): slot 0 = empty, slot 1 = nether quartz, slot 2 = quartz block.
 * (baseSlot=2): slot 0 = empty, slot 1 = empty, slot 2 = item (acts like a normal barrel).
 */
public class CompactingBarrelItemHandler implements ResourceHandler<ItemResource>, IKeyCounterContributor {

    private final BarrelBlockEntity be;

    public CompactingBarrelItemHandler(BarrelBlockEntity be) {
        this.be = be;
    }

    private void cache() {
        be.ensureCompactingCache();
    }

    /** Maps an absolute slot (0–2) to a relative index: 0 = base, 1 = tier1, 2 = tier2. */
    private int rel(int slot) {
        return slot - be.baseSlot;
    }

    @Override
    public int size() {
        return 3;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public ItemResource getResource(int index) {
        cache();
        int r = rel(index);
        if (r < 0) return ItemResource.EMPTY;
        return switch (r) {
            case 0 -> (!be.isLocked() || be.count <= 0) ? ItemResource.EMPTY : ItemResource.of(be.storedItem);
            case 1 -> (be.compactTier1Item.isEmpty() || be.count / be.compactTier1Ratio <= 0)
                    ? ItemResource.EMPTY
                    : ItemResource.of(be.compactTier1Item);
            case 2 -> {
                long combined = (long) be.compactTier1Ratio * be.compactTier2Ratio;
                yield (be.compactTier2Item.isEmpty() || be.count / combined <= 0)
                        ? ItemResource.EMPTY
                        : ItemResource.of(be.compactTier2Item);
            }
            default -> ItemResource.EMPTY;
        };
    }

    @Override
    public long getAmountAsLong(int index) {
        cache();
        int r = rel(index);
        if (r < 0) return 0;
        return switch (r) {
            case 0 -> be.isLocked() ? be.count : 0;
            case 1 -> be.compactTier1Item.isEmpty() ? 0 : be.count / be.compactTier1Ratio;
            case 2 -> {
                long combined = (long) be.compactTier1Ratio * be.compactTier2Ratio;
                yield be.compactTier2Item.isEmpty() ? 0 : be.count / combined;
            }
            default -> 0;
        };
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        cache();
        int r = rel(index);
        if (r < 0) return 0;
        if (r == 0) return be.isLocked() ? be.storedItem.getMaxStackSize() : 64;
        if (r == 1) return be.compactTier1Item.isEmpty() ? 0 : be.compactTier1Item.getMaxStackSize();
        if (r == 2) return be.compactTier2Item.isEmpty() ? 0 : be.compactTier2Item.getMaxStackSize();
        return 0;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        cache();
        if (resource.isEmpty()) return false;
        int r = rel(index);
        if (r < 0) return false;
        if (r == 0) return !be.isLocked() || ItemStack.isSameItemSameComponents(be.storedItem, resource.toStack(1));
        if (r == 1)
            return !be.compactTier1Item.isEmpty()
                    && ItemStack.isSameItemSameComponents(be.compactTier1Item, resource.toStack(1));
        if (r == 2)
            return !be.compactTier2Item.isEmpty()
                    && ItemStack.isSameItemSameComponents(be.compactTier2Item, resource.toStack(1));
        return false;
    }

    // ── Insert ────────────────────────────────────────────────────────────────

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        cache();
        if (resource.isEmpty() || amount <= 0) return 0;
        int r = rel(index);
        if (r < 0) return 0;
        if (r == 0) {
            long rem = be.insert(resource.toStack(amount), amount, false);
            return amount - (int) Math.min(rem, amount);
        }
        if (r == 1) {
            if (be.compactTier1Item.isEmpty()
                    || !ItemStack.isSameItemSameComponents(be.compactTier1Item, resource.toStack(1))) return 0;
            return insertVirtual(resource, amount, be.compactTier1Ratio);
        }
        if (r == 2) {
            if (be.compactTier2Item.isEmpty()
                    || !ItemStack.isSameItemSameComponents(be.compactTier2Item, resource.toStack(1))) return 0;
            return insertVirtual(resource, amount, (long) be.compactTier1Ratio * be.compactTier2Ratio);
        }
        return 0;
    }

    private int insertVirtual(ItemResource resource, int amount, long ratio) {
        if (!be.isLocked()) return 0;
        long space = be.getCapacity() - be.count;
        long maxInsert = be.voidExcess ? amount : Math.min(amount, space / ratio);
        if (maxInsert <= 0) return 0;
        long add = maxInsert * ratio;
        be.count = be.voidExcess ? Math.min(be.count + add, be.getCapacity()) : be.count + add;
        be.notifyChanged();
        return (int) maxInsert;
    }

    // ── Extract ───────────────────────────────────────────────────────────────

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        cache();
        if (resource.isEmpty() || amount <= 0) return 0;
        int r = rel(index);
        if (r < 0) return 0;
        if (r == 0) {
            if (!be.isLocked()) return 0;
            if (!ItemStack.isSameItemSameComponents(be.storedItem, resource.toStack(1))) return 0;
            int cap = (int) Math.min(amount, be.storedItem.getMaxStackSize());
            return be.extract(cap, false).getCount();
        }
        if (r == 1 && !be.compactTier1Item.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(be.compactTier1Item, resource.toStack(1))) return 0;
            return extractVirtual(resource, amount, be.compactTier1Ratio);
        }
        if (r == 2 && !be.compactTier2Item.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(be.compactTier2Item, resource.toStack(1))) return 0;
            return extractVirtual(resource, amount, (long) be.compactTier1Ratio * be.compactTier2Ratio);
        }
        return 0;
    }

    private int extractVirtual(ItemResource resource, int amount, long ratio) {
        if (!be.isLocked()) return 0;
        long available = be.count / ratio;
        if (available <= 0) return 0;
        int maxStackSize = resource.toStack(1).getMaxStackSize();
        int capped = (int) Math.min(amount, Math.min(available, maxStackSize));
        if (capped <= 0) return 0;
        be.count -= (long) capped * ratio;
        be.notifyChanged();
        return capped;
    }

    // ── Simulation helpers ────────────────────────────────────────────────────

    /**
     * Calculates how much of {@code resource} could be inserted into slot {@code index} without
     * actually modifying state. Used internally for puller logic.
     */
    public int simulateInsert(int index, ItemResource resource, int amount) {
        cache();
        if (resource.isEmpty() || amount <= 0) return 0;
        int r = rel(index);
        if (r < 0) return 0;
        if (r == 0) {
            long rem = be.insert(resource.toStack(amount), amount, true);
            return amount - (int) Math.min(rem, amount);
        }
        if (r == 1) {
            if (be.compactTier1Item.isEmpty()
                    || !ItemStack.isSameItemSameComponents(be.compactTier1Item, resource.toStack(1))) return 0;
            return canInsertVirtual(amount, be.compactTier1Ratio);
        }
        if (r == 2) {
            if (be.compactTier2Item.isEmpty()
                    || !ItemStack.isSameItemSameComponents(be.compactTier2Item, resource.toStack(1))) return 0;
            return canInsertVirtual(amount, (long) be.compactTier1Ratio * be.compactTier2Ratio);
        }
        return 0;
    }

    private int canInsertVirtual(int amount, long ratio) {
        if (!be.isLocked()) return 0;
        long space = be.getCapacity() - be.count;
        long maxInsert = be.voidExcess ? amount : Math.min(amount, space / ratio);
        return (int) Math.max(0, maxInsert);
    }

    // ── IKeyCounterContributor ────────────────────────────────────────────────

    @Override
    public void contributeToKeyCounter(KeyCounter kc) {
        cache();
        if (!be.isLocked() || be.count <= 0) return;
        kc.add(StorageKey.of(be.storedItem), be.count);
        if (!be.compactingUpgrade) return;
        if (!be.compactTier1Item.isEmpty() && be.compactTier1Ratio > 0) {
            long n = be.count / be.compactTier1Ratio;
            if (n > 0) kc.add(StorageKey.of(be.compactTier1Item), n);
        }
        if (!be.compactTier2Item.isEmpty() && be.compactTier1Ratio > 0 && be.compactTier2Ratio > 0) {
            long n = be.count / ((long) be.compactTier1Ratio * be.compactTier2Ratio);
            if (n > 0) kc.add(StorageKey.of(be.compactTier2Item), n);
        }
    }
}
