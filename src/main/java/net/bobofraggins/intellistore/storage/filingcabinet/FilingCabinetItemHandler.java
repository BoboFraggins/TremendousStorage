package net.bobofraggins.intellistore.storage.filingcabinet;

import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes the Filing Cabinet's folder contents as an {@link IItemHandler} for use by hoppers,
 * pipes, and any mod that reads inventories via the NeoForge capability system.
 *
 * <p>Slot model: 8 slots, one per folder slot. Each slot presents the <em>stored item</em>
 * inside the folder — not the folder item itself. External automation sees the cabinet as a
 * plain 8-slot inventory where each slot holds the items stored in the corresponding folder.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>Damageable items are always rejected.
 *   <li>If the slot has no folder, the stack is returned unchanged.
 *   <li>If the folder is locked to a matching item, items are inserted up to remaining capacity.
 *   <li>If the folder is unlocked (empty), it is locked to this item type and items are inserted.
 *   <li>If the folder is locked to a different item, the stack is returned unchanged.
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>If the slot has no folder, is unlocked, or has count == 0, EMPTY is returned.
 *   <li>Up to {@code min(amount, maxStackSize, count)} items are extracted.
 *   <li>The folder remains locked to its item type even when drained to count 0.
 * </ol>
 */
public class FilingCabinetItemHandler implements IItemHandler {

    private final FilingCabinetBlockEntity be;

    public FilingCabinetItemHandler(FilingCabinetBlockEntity be) {
        this.be = be;
    }

    /** Returns the server if the BE's level is a server level, else null. */
    private MinecraftServer server() {
        if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) return sl.getServer();
        return null;
    }

    /** Gets the live folder contents, routing through EnderFolderStorage for Ender Folders. */
    private FolderContents getContents(ItemStack folder) {
        if (folder.getItem() instanceof EnderFolderItem) {
            return EnderFolderItem.getLiveContents(folder, server());
        }
        return ManillaFolderItem.getContents(folder);
    }

    /** Writes updated folder contents, propagating to linked items for Ender Folders. */
    private ItemStack setContents(ItemStack folder, FolderContents fc) {
        if (folder.getItem() instanceof EnderFolderItem) {
            ItemStack copy = folder.copyWithCount(1);
            EnderFolderItem.setLiveContents(copy, fc, server());
            return copy;
        }
        return ManillaFolderItem.setContents(folder.copyWithCount(1), fc);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int getSlots() {
        return FilingCabinetBlockEntity.SLOT_COUNT;
    }

    @Override
    public int getSlotLimit(int slot) {
        ItemStack folder = be.getFolder(slot);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem folderItem)) {
            return 0;
        }
        long capacity = folderItem.getCapacity();
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || stack.isDamageableItem()) return false;
        ItemStack folder = be.getFolder(slot);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return false;
        FolderContents contents = getContents(folder);
        return contents.accepts(stack);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — read
    // -------------------------------------------------------------------------

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack folder = be.getFolder(slot);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) {
            return ItemStack.EMPTY;
        }
        FolderContents contents = getContents(folder);
        if (contents.isEmpty() || contents.count() == 0) return ItemStack.EMPTY;
        ItemStack stored = contents.storedItem().get();
        int visible = (int) Math.min(stored.getMaxStackSize(), contents.count());
        return stored.copyWithCount(visible);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — insert
    // -------------------------------------------------------------------------

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.isDamageableItem()) return stack; // rule 1: reject damageable

        ItemStack folder = be.getFolder(slot);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem folderItem)) {
            return stack; // rule 2: no folder here
        }

        FolderContents contents = getContents(folder);
        long capacity = folderItem.getCapacity();

        // Rule 4: unlocked folder — lock it to this item type first
        if (contents.isEmpty()) {
            contents = new FolderContents(java.util.Optional.of(stack.copyWithCount(1)), 0L);
        } else if (!contents.accepts(stack)) {
            return stack; // rule 5: locked to a different item
        }

        // Rules 3 & 4: insert up to remaining capacity
        FolderContents.InsertResult result = contents.insert(stack.getCount(), capacity);
        int remainder = (int) result.remainder();

        if (!simulate) {
            be.notifyFolderContentsChanged(slot, setContents(folder, result.updated()));
        }

        if (be.isVoidExcess()) return ItemStack.EMPTY;
        return remainder == 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    // -------------------------------------------------------------------------
    // IItemHandler — extract
    // -------------------------------------------------------------------------

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        ItemStack folder = be.getFolder(slot);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) {
            return ItemStack.EMPTY;
        }

        FolderContents contents = getContents(folder);
        if (contents.isEmpty() || contents.count() == 0) return ItemStack.EMPTY;

        ItemStack stored = contents.storedItem().get();
        long toExtract = Math.min(amount, Math.min(stored.getMaxStackSize(), contents.count()));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            FolderContents.ExtractResult result = contents.extract(toExtract);
            // Folder stays locked to its item type at count 0
            be.notifyFolderContentsChanged(slot, setContents(folder, result.updated()));
        }

        return stored.copyWithCount((int) toExtract);
    }
}
