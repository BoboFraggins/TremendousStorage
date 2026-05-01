package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shared base for {@link net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetMenu}
 * and {@link net.bobofraggins.tremendousstorage.storage.personalfilingcabinet.PersonalFilingCabinetMenu}.
 *
 * <p>Provides the common 8-folder-slot layout (vertical single column), paired extraction slots,
 * player inventory/hotbar slots, and shift-click logic. Subclasses supply the folder
 * {@link Container}, y-offsets, and any extra {@code ContainerData}.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..7]   Folder slots — each holds one ManillaFolderItem, vertically stacked at
 *                x={@link #FOLDER_X}, y={@link #FOLDER_Y_START} + i×18
 *   <li>[8..15]  Extraction slots — read-only view of each folder's contents at
 *                x={@link #EXTRACTION_X}, same y rows
 *   <li>[16..42] Player main inventory (27 slots)
 *   <li>[43..51] Player hotbar (9 slots)
 * </ul>
 */
public abstract class AbstractFilingCabinetMenu extends AbstractContainerMenu {

    public static final int FOLDER_SLOTS = 8;

    /** Number of rows in each of the two columns. */
    public static final int ROWS_PER_COLUMN = FOLDER_SLOTS / 2;

    /** X positions for the left column (folder + extraction). */
    public static final int FOLDER_X_LEFT = 39;

    public static final int EXTRACTION_X_LEFT = 61;

    /** X position of the 2 px vertical rule that separates the two columns. */
    public static final int COLUMN_RULE_X = 87;

    /** X positions for the right column (folder + extraction). */
    public static final int FOLDER_X_RIGHT = 99;

    public static final int EXTRACTION_X_RIGHT = 121;

    /** Y position of the first folder/extraction row relative to {@code topPos}. */
    public static final int FOLDER_Y_START = 36;

    /** Index of the first extraction slot. */
    public static final int EXTRACTION_START = FOLDER_SLOTS;

    protected static final int INV_START = FOLDER_SLOTS * 2;
    protected static final int INV_END = INV_START + 27;
    protected static final int HOTBAR_START = INV_END;
    protected static final int HOTBAR_END = HOTBAR_START + 9;

    protected AbstractFilingCabinetMenu(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    /**
     * Adds the 8 folder slots (two columns of 4 rows each), paired extraction slots, and the
     * player inventory/hotbar. Slots 0–3 are in the left column; slots 4–7 in the right column.
     *
     * @param folderContainer the container backing the folder slots
     * @param playerInv       the player's inventory
     * @param invY            top-y of the 3×9 player inventory rows (relative to topPos)
     * @param hotbarY         top-y of the hotbar row (relative to topPos)
     */
    protected void addAllSlots(Container folderContainer, Inventory playerInv, int invY, int hotbarY) {
        // Slots 0-7: folder slots — left column (0-3) then right column (4-7)
        Slot[] folderSlots = new Slot[FOLDER_SLOTS];
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            int col = i < ROWS_PER_COLUMN ? FOLDER_X_LEFT : FOLDER_X_RIGHT;
            int row = i % ROWS_PER_COLUMN;
            Slot s = addSlot(new Slot(folderContainer, i, col, FOLDER_Y_START + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ManillaFolderItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
            folderSlots[i] = s;
        }

        // Slots 8-15: extraction slots — left column (8-11) then right column (12-15)
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            int col = i < ROWS_PER_COLUMN ? EXTRACTION_X_LEFT : EXTRACTION_X_RIGHT;
            int row = i % ROWS_PER_COLUMN;
            addSlot(new FolderExtractionSlot(
                    folderSlots[i], col, FOLDER_Y_START + row * 18, playerInv.player.getServer()));
        }

        // Slots 16-42: player main inventory (centred: (176-162)/2 = 7 px)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 7 + col * 18, invY + row * 18));
            }
        }

        // Slots 43-51: hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 7 + col * 18, hotbarY));
        }
    }

    public abstract boolean isVoidExcess();

    public abstract void setVoidExcess(boolean on);

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        if (index < FOLDER_SLOTS) {
            // Folder slot → player inventory
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            return copy;

        } else if (index < INV_START) {
            // Extraction slot → extract from folder and move to player inventory
            FolderExtractionSlot extractSlot = (FolderExtractionSlot) slot;
            ItemStack available = extractSlot.getItem();
            if (available.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = available.copy();
            // Pass a separate copy to moveItemStackTo so it can reduce the count freely
            ItemStack toMove = available.copy();
            if (!moveItemStackTo(toMove, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            int moved = copy.getCount() - toMove.getCount();
            if (moved <= 0) return ItemStack.EMPTY;
            extractSlot.extractAmount(moved);
            return copy;

        } else {
            // Player inventory / hotbar
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();

            if (stack.getItem() instanceof ManillaFolderItem) {
                // Folder item: move into a folder slot
                if (!moveItemStackTo(stack, 0, FOLDER_SLOTS, false)) return ItemStack.EMPTY;
            } else {
                // Non-folder item: insert into locked matching folders
                for (int fi = 0; fi < FOLDER_SLOTS && !stack.isEmpty(); fi++) {
                    Slot folderSlot = slots.get(fi);
                    ItemStack folderItem = folderSlot.getItem();
                    if (folderItem.isEmpty() || !(folderItem.getItem() instanceof ManillaFolderItem)) continue;
                    boolean isEnder = folderItem.getItem() instanceof EnderFolderItem;
                    FolderContents contents = isEnder
                            ? EnderFolderItem.getLiveContents(folderItem, player.getServer())
                            : ManillaFolderItem.getContents(folderItem);
                    if (contents.isEmpty() || !contents.accepts(stack)) continue;
                    FolderContents.InsertResult result =
                            contents.insert(stack.getCount(), ManillaFolderItem.getCapacity(folderItem));
                    int inserted = (int) (stack.getCount() - result.remainder());
                    if (inserted <= 0) continue;
                    ItemStack updatedFolder = folderItem.copy();
                    if (isEnder) {
                        EnderFolderItem.setLiveContents(updatedFolder, result.updated(), player.getServer());
                    } else {
                        updatedFolder = ManillaFolderItem.setContents(updatedFolder, result.updated());
                    }
                    folderSlot.set(updatedFolder);
                    stack.shrink(inserted);
                }
                if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            return copy;
        }
    }
}
