package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import net.bobofraggins.tremendousstorage.shared.storage.IPreferredStorage;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.tank.TankItemAdapter;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The {@link ResourceHandler}{@code <ItemResource>} exposed by a Network Interface block entity.
 *
 * <p>Insertion uses a two-phase algorithm per priority bucket: handlers that already store the
 * item type (implementing {@link IPreferredStorage}) are tried first within each bucket, then
 * all remaining handlers in that bucket. This prevents fragmentation across containers of the
 * same priority.
 *
 * <p>Slot indices are consistent across {@link #getResource} and {@link #extract} — both use
 * insert order — so callers can safely iterate slots and extract by the same index.
 *
 * <p>Slot counts are computed dynamically on every call because underlying handlers (e.g. Chest)
 * report different slot counts as items are inserted or removed.
 *
 * <p>For bulk slot iteration (e.g. during extract scans), prefer {@link #slots()} over repeated
 * flat-index calls: each flat-index call is O(H) due to linear slot resolution, whereas
 * {@link #slots()} amortises that to O(1) per slot.
 */
public class NiItemHandler implements ResourceHandler<ItemResource> {

    private final List<ResourceHandler<ItemResource>> insertOrder;
    private final NavigableMap<Integer, List<ResourceHandler<ItemResource>>> insertBuckets;

    public NiItemHandler(
            List<ResourceHandler<ItemResource>> insertOrder,
            NavigableMap<Integer, List<ResourceHandler<ItemResource>>> insertBuckets) {
        this.insertOrder = List.copyOf(insertOrder);
        this.insertBuckets = insertBuckets;
    }

    // -------------------------------------------------------------------------
    // Slot resolution
    // -------------------------------------------------------------------------

    private record SlotRef(ResourceHandler<ItemResource> handler, int localSlot) {}

    /** A resolved slot: direct references to the underlying handler and its local index. */
    public record SlotView(ResourceHandler<ItemResource> handler, int localSlot) {
        public boolean isFluid() {
            return handler instanceof TankItemAdapter;
        }
    }

    /**
     * Iterates all slots in insert order, yielding a {@link SlotView} per slot.
     * Each view holds a direct reference to the underlying handler and local slot index,
     * so all per-slot operations are O(1) rather than the O(H) cost of flat-index resolution.
     */
    public Iterable<SlotView> slots() {
        return () -> new Iterator<>() {
            private int hi = 0;
            private int ls = 0;
            private int curSlots =
                    insertOrder.isEmpty() ? 0 : insertOrder.get(0).size();

            private void skipEmpty() {
                while (hi < insertOrder.size() && ls >= curSlots) {
                    hi++;
                    ls = 0;
                    curSlots = hi < insertOrder.size() ? insertOrder.get(hi).size() : 0;
                }
            }

            {
                skipEmpty();
            }

            @Override
            public boolean hasNext() {
                return hi < insertOrder.size();
            }

            @Override
            public SlotView next() {
                if (!hasNext()) throw new NoSuchElementException();
                SlotView view = new SlotView(insertOrder.get(hi), ls++);
                skipEmpty();
                return view;
            }
        };
    }

    private static SlotRef resolveSlot(int flatSlot, List<ResourceHandler<ItemResource>> order) {
        if (flatSlot < 0) return null;
        int remaining = flatSlot;
        for (ResourceHandler<ItemResource> h : order) {
            int slots = h.size();
            if (remaining < slots) return new SlotRef(h, remaining);
            remaining -= slots;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Package-private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given global slot index maps to a {@link TankItemAdapter}.
     * Prefer {@link SlotView#isFluid()} when iterating via {@link #slots()} to avoid O(H) cost.
     */
    public boolean isFluidSlot(int globalSlot) {
        SlotRef ref = resolveSlot(globalSlot, insertOrder);
        return ref != null && ref.handler() instanceof TankItemAdapter;
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int size() {
        int total = 0;
        for (ResourceHandler<ItemResource> h : insertOrder) total += h.size();
        return total;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        SlotRef ref = resolveSlot(index, insertOrder);
        return ref == null ? 0 : ref.handler().getCapacityAsLong(ref.localSlot(), resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        SlotRef ref = resolveSlot(index, insertOrder);
        return ref != null && ref.handler().isValid(ref.localSlot(), resource);
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — read
    // -------------------------------------------------------------------------

    @Override
    public ItemResource getResource(int index) {
        SlotRef ref = resolveSlot(index, insertOrder);
        return ref == null ? ItemResource.EMPTY : ref.handler().getResource(ref.localSlot());
    }

    @Override
    public long getAmountAsLong(int index) {
        SlotRef ref = resolveSlot(index, insertOrder);
        return ref == null ? 0 : ref.handler().getAmountAsLong(ref.localSlot());
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — insert (two-phase per priority bucket)
    // -------------------------------------------------------------------------

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        StorageKey key = StorageKey.of(resource.toStack(1));
        int remaining = amount;
        for (List<ResourceHandler<ItemResource>> bucket : insertBuckets.values()) {
            // Phase 1: handlers that already store this item type
            for (ResourceHandler<ItemResource> handler : bucket) {
                if (handler instanceof IPreferredStorage ps && ps.isPreferredFor(key)) {
                    remaining -= tryInsertIntoHandler(handler, resource, remaining, tx);
                    if (remaining <= 0) return amount;
                }
            }
            // Phase 2: handlers that don't already store this item type
            for (ResourceHandler<ItemResource> handler : bucket) {
                if (!(handler instanceof IPreferredStorage ps) || !ps.isPreferredFor(key)) {
                    remaining -= tryInsertIntoHandler(handler, resource, remaining, tx);
                    if (remaining <= 0) return amount;
                }
            }
        }
        return amount - remaining;
    }

    private static int tryInsertIntoHandler(
            ResourceHandler<ItemResource> handler, ItemResource resource, int amount, TransactionContext tx) {
        int remaining = amount;
        int slots = handler.size();
        for (int s = 0; s < slots && remaining > 0; s++) {
            remaining -= handler.insert(s, resource, remaining, tx);
        }
        // Chest empty case: 0 slots but still accepts via slot 0
        if (slots == 0 && remaining > 0) {
            remaining -= handler.insert(0, resource, remaining, tx);
        }
        return amount - remaining;
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — extract
    // -------------------------------------------------------------------------

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        SlotRef ref = resolveSlot(index, insertOrder);
        return ref == null ? 0 : ref.handler().extract(ref.localSlot(), resource, amount, tx);
    }
}
