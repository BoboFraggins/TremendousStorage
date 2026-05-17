package net.bobofraggins.tremendousstorage.storage.tube;

import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A composite {@link ResourceHandler}{@code <ItemResource>} that presents all storage blocks in a
 * tube network as a single unified inventory.
 *
 * <p>Handlers are ordered highest-priority-first (by the caller). Insertion tries each handler in
 * order until the stack is fully consumed, regardless of the slot index supplied by the caller.
 * Extraction and read operations map a flat slot index to the correct underlying handler.
 *
 * <p>Slot counts in the underlying handlers are dynamic (Chest grows/shrinks as items are
 * added), so slot resolution is recalculated on each call.
 *
 * <p>Also holds a reference to the connected {@link NetworkInterfaceBlockEntity}.
 */
public class NetworkItemHandler implements ResourceHandler<ItemResource> {

    private final List<ResourceHandler<ItemResource>> handlers;

    @Nullable
    private final NetworkInterfaceBlockEntity networkInterface;

    public NetworkItemHandler(List<ResourceHandler<ItemResource>> handlers) {
        this(handlers, null);
    }

    public NetworkItemHandler(
            List<ResourceHandler<ItemResource>> handlers, @Nullable NetworkInterfaceBlockEntity networkInterface) {
        this.handlers = List.copyOf(handlers);
        this.networkInterface = networkInterface;
    }

    @Nullable
    public NetworkInterfaceBlockEntity getNetworkInterface() {
        return networkInterface;
    }

    // -------------------------------------------------------------------------
    // Slot resolution
    // -------------------------------------------------------------------------

    private record SlotRef(ResourceHandler<ItemResource> handler, int localSlot) {}

    @Nullable
    private SlotRef resolveSlot(int flatSlot) {
        if (flatSlot < 0) return null;
        int remaining = flatSlot;
        for (ResourceHandler<ItemResource> handler : handlers) {
            int slots = handler.size();
            if (remaining < slots) return new SlotRef(handler, remaining);
            remaining -= slots;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int size() {
        int total = 0;
        for (ResourceHandler<ItemResource> h : handlers) total += h.size();
        return total;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        SlotRef ref = resolveSlot(index);
        return ref == null ? 0 : ref.handler().getCapacityAsLong(ref.localSlot(), resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        SlotRef ref = resolveSlot(index);
        return ref != null && ref.handler().isValid(ref.localSlot(), resource);
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — read
    // -------------------------------------------------------------------------

    @Override
    public ItemResource getResource(int index) {
        SlotRef ref = resolveSlot(index);
        return ref == null ? ItemResource.EMPTY : ref.handler().getResource(ref.localSlot());
    }

    @Override
    public long getAmountAsLong(int index) {
        SlotRef ref = resolveSlot(index);
        return ref == null ? 0 : ref.handler().getAmountAsLong(ref.localSlot());
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — insert (priority-ordered, ignores slot parameter)
    // -------------------------------------------------------------------------

    /**
     * Inserts {@code resource} into the network. The {@code index} parameter is ignored;
     * handlers are tried in priority order (highest first) until the amount is fully consumed.
     */
    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        int remaining = amount;
        for (ResourceHandler<ItemResource> handler : handlers) {
            remaining -= tryInsertIntoHandler(handler, resource, remaining, tx);
            if (remaining <= 0) return amount;
        }
        return amount - remaining;
    }

    /**
     * Attempts to insert {@code resource} into every slot of {@code handler} in sequence.
     * Returns total amount inserted.
     *
     * <p>Special case: if the handler reports 0 slots (e.g. Chest when empty), we still try
     * slot 0 because ChestItemHandler ignores the slot parameter and routes through its own
     * insertion logic.
     */
    private static int tryInsertIntoHandler(
            ResourceHandler<ItemResource> handler, ItemResource resource, int amount, TransactionContext tx) {
        int remaining = amount;
        int slots = handler.size();
        for (int s = 0; s < slots && remaining > 0; s++) {
            remaining -= handler.insert(s, resource, remaining, tx);
        }
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
        SlotRef ref = resolveSlot(index);
        return ref == null ? 0 : ref.handler().extract(ref.localSlot(), resource, amount, tx);
    }
}
