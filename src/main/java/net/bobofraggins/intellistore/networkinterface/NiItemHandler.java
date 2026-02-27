package net.bobofraggins.intellistore.networkinterface;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The {@link IItemHandler} exposed by a Network Interface block entity.
 *
 * <p>Insertion fills the highest-priority storage first; extraction drains
 * the lowest-priority storage first. Both lists span the same physical handlers —
 * they are just in opposite orders.
 *
 * <p>Slot numbering (for {@link #getStackInSlot}, {@link #getSlotLimit},
 * {@link #isItemValid}, and {@link #extractItem}) is based on
 * {@link #insertOrder} for read consistency; extraction internally resolves
 * the same flat slot index but walks handlers in {@link #extractOrder}.
 *
 * <p>Slot resolution uses a prefix-sum array built at construction time so that
 * {@link #getSlots()} and per-slot lookups are O(1) / O(log n) rather than
 * iterating all handlers on every call.
 */
public class NiItemHandler implements IItemHandler {

    private final List<IItemHandler> insertOrder;
    private final List<IItemHandler> extractOrder;

    /**
     * Prefix sums for insertOrder: {@code insertPrefixSums[i]} is the total slot count
     * of handlers 0..i-1. Length is {@code insertOrder.size() + 1}; the last entry is
     * the total slot count of the whole network.
     */
    private final int[] insertPrefixSums;
    /** Same structure for extractOrder. */
    private final int[] extractPrefixSums;

    public NiItemHandler(List<IItemHandler> insertOrder) {
        this.insertOrder = List.copyOf(insertOrder);
        List<IItemHandler> rev = new ArrayList<>(insertOrder);
        Collections.reverse(rev);
        this.extractOrder = List.copyOf(rev);

        this.insertPrefixSums  = buildPrefixSums(this.insertOrder);
        this.extractPrefixSums = buildPrefixSums(this.extractOrder);
    }

    private static int[] buildPrefixSums(List<IItemHandler> handlers) {
        int[] sums = new int[handlers.size() + 1];
        for (int i = 0; i < handlers.size(); i++) {
            sums[i + 1] = sums[i] + handlers.get(i).getSlots();
        }
        return sums;
    }

    // -------------------------------------------------------------------------
    // Slot resolution helpers
    // -------------------------------------------------------------------------

    private record SlotRef(IItemHandler handler, int localSlot) {}

    /**
     * Maps a flat slot index to a specific handler + local slot using binary search
     * on the prefix-sum array — O(log n) in the number of handlers.
     */
    private SlotRef resolveSlot(int flatSlot, List<IItemHandler> order, int[] prefixSums) {
        if (flatSlot < 0 || flatSlot >= prefixSums[prefixSums.length - 1]) return null;
        // Binary search: find largest i such that prefixSums[i] <= flatSlot
        int lo = 0, hi = order.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (prefixSums[mid] <= flatSlot) lo = mid; else hi = mid - 1;
        }
        return new SlotRef(order.get(lo), flatSlot - prefixSums[lo]);
    }

    /** Maps a flat slot index (in insertOrder layout) to the specific handler + local slot. */
    private SlotRef resolveSlotInsert(int flatSlot) {
        return resolveSlot(flatSlot, insertOrder, insertPrefixSums);
    }

    /** Maps a flat slot index (in extractOrder layout) to the specific handler + local slot. */
    private SlotRef resolveSlotExtract(int flatSlot) {
        return resolveSlot(flatSlot, extractOrder, extractPrefixSums);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int getSlots() {
        // O(1): last entry of the prefix-sum array is the total
        return insertPrefixSums[insertPrefixSums.length - 1];
    }

    @Override
    public int getSlotLimit(int slot) {
        SlotRef ref = resolveSlotInsert(slot);
        return ref == null ? 0 : ref.handler().getSlotLimit(ref.localSlot());
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        SlotRef ref = resolveSlotInsert(slot);
        return ref != null && ref.handler().isItemValid(ref.localSlot(), stack);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — read
    // -------------------------------------------------------------------------

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        SlotRef ref = resolveSlotInsert(slot);
        return ref == null ? ItemStack.EMPTY : ref.handler().getStackInSlot(ref.localSlot());
    }

    // -------------------------------------------------------------------------
    // IItemHandler — insert (highest-priority first, slot param ignored)
    // -------------------------------------------------------------------------

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack;
        for (IItemHandler handler : insertOrder) {
            remaining = tryInsertIntoHandler(handler, remaining, simulate);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        return remaining;
    }

    private static ItemStack tryInsertIntoHandler(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        int slots = handler.getSlots();
        for (int s = 0; s < slots && !remaining.isEmpty(); s++) {
            remaining = handler.insertItem(s, remaining, simulate);
        }
        // BulkStorageContainer empty case: 0 slots but still accepts via slot 0
        if (slots == 0 && !remaining.isEmpty()) {
            remaining = handler.insertItem(0, remaining, simulate);
        }
        return remaining;
    }

    // -------------------------------------------------------------------------
    // IItemHandler — extract (lowest-priority first via extractOrder)
    // -------------------------------------------------------------------------

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        SlotRef ref = resolveSlotExtract(slot);
        return ref == null ? ItemStack.EMPTY : ref.handler().extractItem(ref.localSlot(), amount, simulate);
    }
}
