package net.bobofraggins.tremendousstorage.storage.filingcabinet;

import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.IPreferredStorage;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Filing Cabinet's folder contents as a {@link ResourceHandler}{@code <ItemResource>}
 * for use by hoppers, pipes, and any mod that reads inventories via the NeoForge capability system.
 *
 * <p>Slot model: 8 slots, one per folder slot. Each slot presents the <em>stored item</em>
 * inside the folder — not the folder item itself.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>Damageable items are always rejected.
 *   <li>If the slot has no folder, nothing is inserted.
 *   <li>If the folder is locked to a matching item, items are inserted up to remaining capacity.
 *   <li>If the folder is unlocked (empty), it is locked to this item type and items are inserted.
 *   <li>If the folder is locked to a different item, nothing is inserted.
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>If the slot has no folder, is unlocked, or has count == 0, returns 0.
 *   <li>Up to {@code min(amount, maxStackSize, count)} items are extracted.
 *   <li>The folder remains locked to its item type even when drained to count 0.
 * </ol>
 */
public class FilingCabinetItemHandler
        implements ResourceHandler<ItemResource>, IKeyCounterContributor, IPreferredStorage {

    private final FilingCabinetBlockEntity be;

    public FilingCabinetItemHandler(FilingCabinetBlockEntity be) {
        this.be = be;
    }

    private MinecraftServer server() {
        if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) return sl.getServer();
        return null;
    }

    private FolderContents getContents(ItemStack folder) {
        if (folder.getItem() instanceof EnderFolderItem) {
            return EnderFolderItem.getLiveContents(folder, server());
        }
        return ManillaFolderItem.getContents(folder);
    }

    private ItemStack setContents(ItemStack folder, FolderContents fc) {
        if (folder.getItem() instanceof EnderFolderItem) {
            ItemStack copy = folder.copyWithCount(1);
            EnderFolderItem.setLiveContents(copy, fc, server());
            return copy;
        }
        return ManillaFolderItem.setContents(folder.copyWithCount(1), fc);
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — metadata
    // -------------------------------------------------------------------------

    @Override
    public int size() {
        return FilingCabinetBlockEntity.SLOT_COUNT;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        ItemStack folder = be.getFolder(index);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return 0;
        return ManillaFolderItem.getCapacity(folder);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.isEmpty() || resource.toStack(1).isDamageableItem()) return false;
        ItemStack folder = be.getFolder(index);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return false;
        FolderContents contents = getContents(folder);
        return contents.accepts(resource.toStack(1));
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — read
    // -------------------------------------------------------------------------

    @Override
    public ItemResource getResource(int index) {
        ItemStack folder = be.getFolder(index);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return ItemResource.EMPTY;
        FolderContents contents = getContents(folder);
        if (contents.isEmpty() || contents.count() == 0) return ItemResource.EMPTY;
        return contents.storedItem().map(ItemResource::of).orElse(ItemResource.EMPTY);
    }

    @Override
    public long getAmountAsLong(int index) {
        ItemStack folder = be.getFolder(index);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return 0;
        FolderContents contents = getContents(folder);
        if (contents.isEmpty()) return 0;
        return contents.count();
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — insert
    // -------------------------------------------------------------------------

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack stack = resource.toStack(1);
        if (stack.isDamageableItem()) return 0;

        ItemStack folder = be.getFolder(index);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return 0;

        FolderContents contents = getContents(folder);
        long capacity = ManillaFolderItem.getCapacity(folder);

        if (contents.isEmpty()) {
            contents = new FolderContents(java.util.Optional.of(stack.copyWithCount(1)), 0L, contents.tier());
        } else if (!contents.accepts(stack)) {
            return 0;
        }

        FolderContents.InsertResult result = contents.insert(amount, capacity);
        int remainder = (int) result.remainder();

        be.notifyFolderContentsChanged(index, setContents(folder, result.updated()));

        if (be.isVoidExcess()) return amount;
        return amount - remainder;
    }

    // -------------------------------------------------------------------------
    // ResourceHandler — extract
    // -------------------------------------------------------------------------

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (resource.isEmpty() || amount <= 0) return 0;

        ItemStack folder = be.getFolder(index);
        if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) return 0;

        FolderContents contents = getContents(folder);
        if (contents.isEmpty() || contents.count() == 0) return 0;
        if (contents.storedItem().isEmpty()) return 0;

        ItemStack stored = contents.storedItem().get();
        if (!ItemStack.isSameItemSameComponents(stored, resource.toStack(1))) return 0;

        long toExtract = Math.min(amount, Math.min(stored.getMaxStackSize(), contents.count()));
        if (toExtract <= 0) return 0;

        FolderContents.ExtractResult result = contents.extract(toExtract);
        be.notifyFolderContentsChanged(index, setContents(folder, result.updated()));

        return (int) toExtract;
    }

    // -------------------------------------------------------------------------
    // IKeyCounterContributor
    // -------------------------------------------------------------------------

    @Override
    public void contributeToKeyCounter(KeyCounter kc) {
        for (int slot = 0; slot < FilingCabinetBlockEntity.SLOT_COUNT; slot++) {
            ItemStack folder = be.getFolder(slot);
            if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) continue;
            FolderContents contents = getContents(folder);
            if (contents.isEmpty() || contents.count() == 0) continue;
            if (contents.storedItem().isEmpty()) continue;
            ItemStack stored = contents.storedItem().get();
            kc.add(StorageKey.of(stored), contents.count());
        }
    }

    // -------------------------------------------------------------------------
    // IPreferredStorage
    // -------------------------------------------------------------------------

    @Override
    public boolean isPreferredFor(StorageKey key) {
        ItemStack probe = key.toDisplayStack();
        for (int slot = 0; slot < FilingCabinetBlockEntity.SLOT_COUNT; slot++) {
            ItemStack folder = be.getFolder(slot);
            if (folder.isEmpty() || !(folder.getItem() instanceof ManillaFolderItem)) continue;
            FolderContents contents = getContents(folder);
            if (!contents.isEmpty() && contents.accepts(probe)) return true;
        }
        return false;
    }
}
