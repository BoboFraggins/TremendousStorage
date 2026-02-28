package net.bobofraggins.intellistore.storage.tube;

import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A composite {@link IItemHandler} that presents all storage blocks in a tube network
 * as a single unified inventory.
 *
 * <p>Handlers are ordered highest-priority-first (by the caller). Insertion tries each
 * handler in order until the stack is fully consumed, regardless of the slot index supplied
 * by the caller. Extraction and read operations map a flat slot index to the correct
 * underlying handler.
 *
 * <p>Slot counts in the underlying handlers are dynamic (JunkDrawer, BulkStorageContainer
 * grow/shrink as items are added), so slot resolution is recalculated on each call.
 *
 * <p>Also holds a reference to the connected {@link NetworkInterfaceBlockEntity} so that
 * {@link TubeEnergyHandler} can route incoming energy to the correct NI buffer.
 */
public class NetworkItemHandler implements IItemHandler {

    private final List<IItemHandler> handlers;

    /** The NI that manages this network, or {@code null} if not found. */
    @Nullable
    private final NetworkInterfaceBlockEntity networkInterface;

    public NetworkItemHandler(List<IItemHandler> handlers) {
        this(handlers, null);
    }

    public NetworkItemHandler(List<IItemHandler> handlers, @Nullable NetworkInterfaceBlockEntity networkInterface) {
        this.handlers = List.copyOf(handlers);
        this.networkInterface = networkInterface;
    }

    /** Returns the Network Interface managing this network, or {@code null}. */
    @Nullable
    public NetworkInterfaceBlockEntity getNetworkInterface() {
        return networkInterface;
    }

    // -------------------------------------------------------------------------
    // Slot resolution
    // -------------------------------------------------------------------------

    private record SlotRef(IItemHandler handler, int localSlot) {}

    /**
     * Maps a flat slot index across all handlers (in list order) to the specific
     * (handler, localSlot) pair. Returns {@code null} if the index is out of range.
     */
    @Nullable
    private SlotRef resolveSlot(int flatSlot) {
        if (flatSlot < 0) return null;
        int remaining = flatSlot;
        for (IItemHandler handler : handlers) {
            int slots = handler.getSlots();
            if (remaining < slots) {
                return new SlotRef(handler, remaining);
            }
            remaining -= slots;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // IItemHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int getSlots() {
        int total = 0;
        for (IItemHandler h : handlers) {
            total += h.getSlots();
        }
        return total;
    }

    @Override
    public int getSlotLimit(int slot) {
        SlotRef ref = resolveSlot(slot);
        return ref == null ? 0 : ref.handler().getSlotLimit(ref.localSlot());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        SlotRef ref = resolveSlot(slot);
        return ref != null && ref.handler().isItemValid(ref.localSlot(), stack);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — read
    // -------------------------------------------------------------------------

    @Override
    public ItemStack getStackInSlot(int slot) {
        SlotRef ref = resolveSlot(slot);
        return ref == null ? ItemStack.EMPTY : ref.handler().getStackInSlot(ref.localSlot());
    }

    // -------------------------------------------------------------------------
    // IItemHandler — insert (priority-ordered, ignores slot parameter)
    // -------------------------------------------------------------------------

    /**
     * Inserts {@code stack} into the network. The {@code slot} parameter is ignored;
     * handlers are tried in priority order (highest first) until the stack is fully consumed.
     *
     * <p>This matches AE2/RS network insertion semantics: the network decides which physical
     * storage to fill, not the caller.
     */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack;
        for (IItemHandler handler : handlers) {
            remaining = tryInsertIntoHandler(handler, remaining, simulate);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        return remaining;
    }

    /**
     * Attempts to insert {@code stack} into every slot of {@code handler} in sequence.
     * Returns whatever could not be inserted.
     *
     * <p>Special case: if the handler reports 0 slots (e.g. BulkStorageContainer when
     * empty — its slot count equals the number of distinct item types stored), we still
     * try slot 0 because BulkStorageContainerItemHandler ignores the slot parameter and
     * routes through its own insertion logic.
     */
    private static ItemStack tryInsertIntoHandler(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        int slots = handler.getSlots();
        for (int s = 0; s < slots && !remaining.isEmpty(); s++) {
            remaining = handler.insertItem(s, remaining, simulate);
        }
        if (slots == 0 && !remaining.isEmpty()) {
            remaining = handler.insertItem(0, remaining, simulate);
        }
        return remaining;
    }

    // -------------------------------------------------------------------------
    // IItemHandler — extract
    // -------------------------------------------------------------------------

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        SlotRef ref = resolveSlot(slot);
        return ref == null ? ItemStack.EMPTY : ref.handler().extractItem(ref.localSlot(), amount, simulate);
    }
}
