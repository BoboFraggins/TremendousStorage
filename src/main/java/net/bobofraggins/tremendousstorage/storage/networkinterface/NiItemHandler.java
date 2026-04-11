package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.List;
import java.util.NavigableMap;
import javax.annotation.Nonnull;
import net.bobofraggins.tremendousstorage.shared.storage.IPreferredStorage;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.tank.TankItemAdapter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The {@link IItemHandler} exposed by a Network Interface block entity.
 *
 * <p>Insertion uses a two-phase algorithm per priority bucket: handlers that already store the
 * item type (implementing {@link IPreferredStorage}) are tried first within each bucket, then
 * all remaining handlers in that bucket. This prevents fragmentation across containers of the
 * same priority.
 *
 * <p>Slot indices are consistent across {@link #getStackInSlot} and {@link #extractItem} — both
 * use insert order — so callers can safely iterate slots and extract by the same index.
 *
 * <p>Slot counts are computed dynamically on every call because underlying handlers
 * (e.g. Chest) report different slot counts as items are inserted or
 * removed. Caching slot counts at construction time would cause newly-inserted items to
 * be invisible until the next full network re-scan.
 */
public class NiItemHandler implements IItemHandler {

    private final List<IItemHandler> insertOrder;
    private final NavigableMap<Integer, List<IItemHandler>> insertBuckets;

    public NiItemHandler(List<IItemHandler> insertOrder, NavigableMap<Integer, List<IItemHandler>> insertBuckets) {
        this.insertOrder = List.copyOf(insertOrder);
        this.insertBuckets = insertBuckets;
    }

    // -------------------------------------------------------------------------
    // Slot resolution
    // -------------------------------------------------------------------------

    private record SlotRef(IItemHandler handler, int localSlot) {}

    /**
     * Maps a flat slot index to a handler + local slot by walking the handler list.
     * Dynamic (not cached) so that handlers with variable slot counts are handled correctly.
     */
    private static SlotRef resolveSlot(int flatSlot, List<IItemHandler> order) {
        if (flatSlot < 0) return null;
        int remaining = flatSlot;
        for (IItemHandler h : order) {
            int slots = h.getSlots();
            if (remaining < slots) {
                return new SlotRef(h, remaining);
            }
            remaining -= slots;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Package-private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given global slot index maps to a {@link TankItemAdapter}.
     * Used by {@link net.bobofraggins.tremendousstorage.shared.network.SatExtractPacket} to enforce
     * the empty-bucket requirement before extracting fluid.
     */
    public boolean isFluidSlot(int globalSlot) {
        SlotRef ref = resolveSlot(globalSlot, insertOrder);
        return ref != null && ref.handler() instanceof TankItemAdapter;
    }

    // -------------------------------------------------------------------------
    // IItemHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int getSlots() {
        int total = 0;
        for (IItemHandler h : insertOrder) {
            total += h.getSlots();
        }
        return total;
    }

    @Override
    public int getSlotLimit(int slot) {
        SlotRef ref = resolveSlot(slot, insertOrder);
        return ref == null ? 0 : ref.handler().getSlotLimit(ref.localSlot());
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        SlotRef ref = resolveSlot(slot, insertOrder);
        return ref != null && ref.handler().isItemValid(ref.localSlot(), stack);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — read
    // -------------------------------------------------------------------------

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        SlotRef ref = resolveSlot(slot, insertOrder);
        return ref == null ? ItemStack.EMPTY : ref.handler().getStackInSlot(ref.localSlot());
    }

    // -------------------------------------------------------------------------
    // IItemHandler — insert (two-phase per priority bucket)
    // -------------------------------------------------------------------------

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        StorageKey key = StorageKey.of(stack);
        ItemStack remaining = stack;
        // Two-phase insert per priority bucket: preferred handlers first, then all others
        for (List<IItemHandler> bucket : insertBuckets.values()) {
            // Phase 1: handlers that already store this item type
            for (IItemHandler handler : bucket) {
                if (handler instanceof IPreferredStorage ps && ps.isPreferredFor(key)) {
                    remaining = tryInsertIntoHandler(handler, remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
            // Phase 2: handlers that don't already store this item type
            for (IItemHandler handler : bucket) {
                if (!(handler instanceof IPreferredStorage ps) || !ps.isPreferredFor(key)) {
                    remaining = tryInsertIntoHandler(handler, remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        return remaining;
    }

    private static ItemStack tryInsertIntoHandler(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        int slots = handler.getSlots();
        for (int s = 0; s < slots && !remaining.isEmpty(); s++) {
            remaining = handler.insertItem(s, remaining, simulate);
        }
        // Chest empty case: 0 slots but still accepts via slot 0
        if (slots == 0 && !remaining.isEmpty()) {
            remaining = handler.insertItem(0, remaining, simulate);
        }
        return remaining;
    }

    // -------------------------------------------------------------------------
    // IItemHandler — extract
    // -------------------------------------------------------------------------

    /**
     * Extracts from the slot at the given index in insert order (same mapping as
     * {@link #getStackInSlot}), so callers that iterate slots via getStackInSlot can
     * extract from the same slot using the same index.
     */
    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        SlotRef ref = resolveSlot(slot, insertOrder);
        return ref == null ? ItemStack.EMPTY : ref.handler().extractItem(ref.localSlot(), amount, simulate);
    }
}
