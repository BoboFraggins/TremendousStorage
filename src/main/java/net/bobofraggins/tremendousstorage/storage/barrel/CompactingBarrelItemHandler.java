package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Three-slot {@link IItemHandler} for compacting barrels.
 *
 * <p>Slots 0–2 map to IItemHandler slots 0–2. {@code baseSlot} indicates which logical slot holds
 * the real stored item; slots below it are always empty and reject all operations. Slots at and
 * above {@code baseSlot} expose compressed tiers.
 *
 * <p>Examples (baseSlot=0): slot 0 = nuggets, slot 1 = ingots, slot 2 = blocks.
 * (baseSlot=1): slot 0 = empty, slot 1 = nether quartz, slot 2 = quartz block.
 * (baseSlot=2): slot 0 = empty, slot 1 = empty, slot 2 = item (acts like a normal barrel).
 */
public class CompactingBarrelItemHandler implements IItemHandler, IKeyCounterContributor {

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
    public int getSlots() {
        return 3;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        cache();
        int r = rel(slot);
        if (r < 0) return ItemStack.EMPTY;
        return switch (r) {
            case 0 -> {
                if (!be.isLocked() || be.count <= 0) yield ItemStack.EMPTY;
                int vis = (int) Math.min(be.count, be.storedItem.getMaxStackSize());
                yield be.storedItem.copyWithCount(vis);
            }
            case 1 -> {
                if (be.compactTier1Item.isEmpty()) yield ItemStack.EMPTY;
                long n = be.count / be.compactTier1Ratio;
                if (n <= 0) yield ItemStack.EMPTY;
                yield be.compactTier1Item.copyWithCount((int) Math.min(n, be.compactTier1Item.getMaxStackSize()));
            }
            case 2 -> {
                if (be.compactTier2Item.isEmpty()) yield ItemStack.EMPTY;
                long combined = (long) be.compactTier1Ratio * be.compactTier2Ratio;
                long n = be.count / combined;
                if (n <= 0) yield ItemStack.EMPTY;
                yield be.compactTier2Item.copyWithCount((int) Math.min(n, be.compactTier2Item.getMaxStackSize()));
            }
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int getSlotLimit(int slot) {
        cache();
        int r = rel(slot);
        if (r < 0) return 0;
        if (r == 0) return be.isLocked() ? be.storedItem.getMaxStackSize() : 64;
        if (r == 1) return be.compactTier1Item.isEmpty() ? 0 : be.compactTier1Item.getMaxStackSize();
        if (r == 2) return be.compactTier2Item.isEmpty() ? 0 : be.compactTier2Item.getMaxStackSize();
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        cache();
        if (stack.isEmpty()) return false;
        int r = rel(slot);
        if (r < 0) return false;
        if (r == 0) return !be.isLocked() || ItemStack.isSameItemSameComponents(be.storedItem, stack);
        if (r == 1)
            return !be.compactTier1Item.isEmpty() && ItemStack.isSameItemSameComponents(be.compactTier1Item, stack);
        if (r == 2)
            return !be.compactTier2Item.isEmpty() && ItemStack.isSameItemSameComponents(be.compactTier2Item, stack);
        return false;
    }

    // ── Insert ────────────────────────────────────────────────────────────────

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        cache();
        if (stack.isEmpty()) return stack;
        int r = rel(slot);
        if (r < 0) return stack;
        if (r == 0) {
            long rem = be.insert(stack, stack.getCount(), simulate);
            return rem <= 0 ? ItemStack.EMPTY : stack.copyWithCount((int) Math.min(rem, Integer.MAX_VALUE));
        }
        if (r == 1) {
            if (be.compactTier1Item.isEmpty() || !ItemStack.isSameItemSameComponents(be.compactTier1Item, stack))
                return stack;
            return insertVirtual(stack, be.compactTier1Ratio, simulate);
        }
        if (r == 2) {
            if (be.compactTier2Item.isEmpty() || !ItemStack.isSameItemSameComponents(be.compactTier2Item, stack))
                return stack;
            return insertVirtual(stack, (long) be.compactTier1Ratio * be.compactTier2Ratio, simulate);
        }
        return stack;
    }

    private @NotNull ItemStack insertVirtual(@NotNull ItemStack stack, long ratio, boolean simulate) {
        if (!be.isLocked()) return stack;
        long toInsert = stack.getCount();
        long space = be.getCapacity() - be.count;
        long maxInsert = be.voidExcess ? toInsert : Math.min(toInsert, space / ratio);
        if (maxInsert <= 0) return stack;
        if (!simulate) {
            long add = maxInsert * ratio;
            be.count = be.voidExcess ? Math.min(be.count + add, be.getCapacity()) : be.count + add;
            be.notifyChanged();
        }
        long rem = toInsert - maxInsert;
        return rem > 0 ? stack.copyWithCount((int) rem) : ItemStack.EMPTY;
    }

    // ── Extract ───────────────────────────────────────────────────────────────

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        cache();
        if (amount <= 0) return ItemStack.EMPTY;
        int r = rel(slot);
        if (r < 0) return ItemStack.EMPTY;
        if (r == 0) {
            int cap = be.isLocked() ? (int) Math.min(amount, be.storedItem.getMaxStackSize()) : 0;
            return be.extract(cap, simulate);
        }
        if (r == 1 && !be.compactTier1Item.isEmpty())
            return extractVirtual(be.compactTier1Item, amount, be.compactTier1Ratio, simulate);
        if (r == 2 && !be.compactTier2Item.isEmpty())
            return extractVirtual(
                    be.compactTier2Item, amount, (long) be.compactTier1Ratio * be.compactTier2Ratio, simulate);
        return ItemStack.EMPTY;
    }

    private @NotNull ItemStack extractVirtual(ItemStack type, int amount, long ratio, boolean simulate) {
        if (!be.isLocked()) return ItemStack.EMPTY;
        long available = be.count / ratio;
        if (available <= 0) return ItemStack.EMPTY;
        int capped = (int) Math.min(amount, Math.min(available, type.getMaxStackSize()));
        if (capped <= 0) return ItemStack.EMPTY;
        if (!simulate) {
            be.count -= (long) capped * ratio;
            be.notifyChanged();
        }
        return type.copyWithCount(capped);
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
