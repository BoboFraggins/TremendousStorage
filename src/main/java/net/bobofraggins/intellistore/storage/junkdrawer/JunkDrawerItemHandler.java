package net.bobofraggins.intellistore.storage.junkdrawer;

import net.bobofraggins.intellistore.shared.storage.IKeyCounterContributor;
import net.bobofraggins.intellistore.shared.storage.KeyCounter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes the Junk Drawer as a dynamic-slot {@link IItemHandler} for automation.
 *
 * <p>Slot count equals the number of items currently stored (one slot per item). Slots are
 * indexed 0..size-1 and each holds exactly one item (stack size 1). The slot count changes as
 * items are inserted and extracted — automation mods such as Applied Energistics handle this
 * correctly by re-querying slot count on each interaction.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>Items that are neither damageable nor carry non-default component data are refused.
 *   <li>If the drawer is full (32,768 items), the entire stack is returned.
 *   <li>Each item in the incoming stack is inserted individually (count 1 per slot) until
 *       capacity is reached; any remainder is returned.
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>Extracts the item at the given slot index (count 1).
 *   <li>Out-of-range slot indices return EMPTY.
 * </ol>
 */
public class JunkDrawerItemHandler implements IItemHandler, IKeyCounterContributor {

    private final JunkDrawerBlockEntity be;

    public JunkDrawerItemHandler(JunkDrawerBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getSlots() {
        return be.size();
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return JunkDrawerBlockEntity.accepts(stack);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return be.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!JunkDrawerBlockEntity.accepts(stack)) return stack;

        int toInsert = stack.getCount();
        int canInsert = Math.min(toInsert, (int) Math.min(Integer.MAX_VALUE, be.getCapacity()) - be.size());
        if (canInsert <= 0) return stack;

        if (!simulate) {
            for (int i = 0; i < canInsert; i++) {
                be.addItem(stack);
            }
        }

        int remainder = toInsert - canInsert;
        return remainder == 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= be.size()) return ItemStack.EMPTY;
        if (simulate) return be.get(slot).copy();
        return be.removeItem(slot);
    }

    @Override
    public void contributeToKeyCounter(KeyCounter kc) {
        be.populateKeyCounter(kc);
    }
}
