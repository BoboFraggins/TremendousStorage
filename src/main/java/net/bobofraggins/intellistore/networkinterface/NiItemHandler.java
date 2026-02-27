package net.bobofraggins.intellistore.networkinterface;

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
 */
public class NiItemHandler implements IItemHandler {

    private final List<IItemHandler> insertOrder;
    private final List<IItemHandler> extractOrder;

    public NiItemHandler(List<IItemHandler> insertOrder) {
        this.insertOrder = List.copyOf(insertOrder);
        List<IItemHandler> rev = new ArrayList<>(insertOrder);
        Collections.reverse(rev);
        this.extractOrder = List.copyOf(rev);
    }

    // -------------------------------------------------------------------------
    // Slot resolution helpers
    // -------------------------------------------------------------------------

    private record SlotRef(IItemHandler handler, int localSlot) {}

    /** Maps a flat slot index (in insertOrder layout) to the specific handler + local slot. */
    private SlotRef resolveSlotInsert(int flatSlot) {
        if (flatSlot < 0) return null;
        int remaining = flatSlot;
        for (IItemHandler h : insertOrder) {
            int slots = h.getSlots();
            if (remaining < slots) return new SlotRef(h, remaining);
            remaining -= slots;
        }
        return null;
    }

    /** Maps a flat slot index (in extractOrder layout) to the specific handler + local slot. */
    private SlotRef resolveSlotExtract(int flatSlot) {
        if (flatSlot < 0) return null;
        int remaining = flatSlot;
        for (IItemHandler h : extractOrder) {
            int slots = h.getSlots();
            if (remaining < slots) return new SlotRef(h, remaining);
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
        for (IItemHandler h : insertOrder) total += h.getSlots();
        return total;
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
