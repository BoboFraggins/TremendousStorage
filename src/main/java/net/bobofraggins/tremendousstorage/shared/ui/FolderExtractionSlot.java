package net.bobofraggins.tremendousstorage.shared.ui;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A read-only extraction slot paired with a folder slot.
 *
 * <p>Displays the item type and extractable quantity stored inside the folder:
 * <ul>
 *   <li>Folder locked with items → shows up to one stack of that item; left/right click extracts.
 *   <li>Folder locked but empty → shows a ghost (dimmed) icon; clicking does nothing.
 *   <li>Folder unlocked and empty → shows nothing.
 * </ul>
 *
 * <p>{@link #set} is intentionally a no-op; all extraction goes through {@link #remove} (direct
 * click) or {@link #extractAmount} (shift-click via
 * {@link AbstractFilingCabinetMenu#quickMoveStack}).
 */
public class FolderExtractionSlot extends Slot {

    private final Slot folderSlot;

    public FolderExtractionSlot(Slot folderSlot, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.folderSlot = folderSlot;
    }

    // -------------------------------------------------------------------------
    // Slot overrides
    // -------------------------------------------------------------------------

    @Override
    public ItemStack getItem() {
        FolderContents contents = getContents();
        if (contents == null || contents.count() == 0 || contents.storedItem().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return contents.storedItem().get().copyWithCount((int) Math.min(Integer.MAX_VALUE, contents.count()));
    }

    @Override
    public boolean hasItem() {
        FolderContents contents = getContents();
        return contents != null && contents.count() > 0 && contents.storedItem().isPresent();
    }

    /**
     * No-op — extraction is handled exclusively by {@link #remove} and {@link #extractAmount}.
     * The server-to-client slot sync calls this; the client reads state directly from the folder
     * slot instead.
     */
    @Override
    public void set(ItemStack stack) {}

    @Override
    public ItemStack remove(int amount) {
        FolderContents contents = getContents();
        if (contents == null || contents.count() == 0 || contents.storedItem().isEmpty()) {
            return ItemStack.EMPTY;
        }
        int capped = Math.min(amount, contents.storedItem().get().getMaxStackSize());
        FolderContents.ExtractResult result = contents.extract(capped);
        if (result.extracted() == 0) return ItemStack.EMPTY;
        writeFolderContents(result.updated());
        return contents.storedItem().get().copyWithCount((int) result.extracted());
    }

    /** Disallow placing items into this slot. */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /**
     * Disallow picking up when the cursor already holds an item, preventing accidental swaps that
     * would silently consume the cursor item.
     */
    @Override
    public boolean mayPickup(Player player) {
        return player.containerMenu.getCarried().isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        FolderContents contents = getContents();
        if (contents == null) return 0;
        return contents.storedItem().map(ItemStack::getMaxStackSize).orElse(0);
    }

    // -------------------------------------------------------------------------
    // Ghost helpers (used by AbstractFilingCabinetScreen)
    // -------------------------------------------------------------------------

    /** Returns {@code true} when the folder is locked to an item type but currently stores zero. */
    public boolean isGhost() {
        FolderContents contents = getContents();
        return contents != null && !contents.isEmpty() && contents.count() == 0;
    }

    /** The locked item type for ghost display, or {@link ItemStack#EMPTY} if not a ghost slot. */
    public ItemStack getGhostItem() {
        FolderContents contents = getContents();
        if (contents == null) return ItemStack.EMPTY;
        return contents.storedItem().map(i -> i.copyWithCount(1)).orElse(ItemStack.EMPTY);
    }

    // -------------------------------------------------------------------------
    // Direct extraction (called from AbstractFilingCabinetMenu.quickMoveStack)
    // -------------------------------------------------------------------------

    void extractAmount(long amount) {
        FolderContents contents = getContents();
        if (contents == null || amount <= 0) return;
        writeFolderContents(contents.extract(amount).updated());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    @Nullable
    private FolderContents getContents() {
        ItemStack folder = folderSlot.getItem();
        if (folder.isEmpty()) return null;
        return folder.get(FolderContents.type());
    }

    private void writeFolderContents(FolderContents updated) {
        ItemStack folder = folderSlot.getItem();
        if (folder.isEmpty()) return;
        folder.set(FolderContents.type(), updated);
        folderSlot.setChanged();
    }
}
