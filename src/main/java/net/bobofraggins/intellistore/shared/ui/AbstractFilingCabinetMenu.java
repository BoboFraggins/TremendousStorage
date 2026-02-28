package net.bobofraggins.intellistore.shared.ui;

import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shared base for {@link net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetMenu}
 * and {@link net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetMenu}.
 *
 * <p>Provides the common 8-folder-slot layout, player inventory/hotbar slots, and
 * shift-click logic. Subclasses supply the folder {@link Container}, y-offsets, and
 * any extra {@code ContainerData}.
 */
public abstract class AbstractFilingCabinetMenu extends AbstractContainerMenu {

    public static final int FOLDER_SLOTS = 8;
    protected static final int INV_START = FOLDER_SLOTS;
    protected static final int INV_END = INV_START + 27;
    protected static final int HOTBAR_START = INV_END;
    protected static final int HOTBAR_END = HOTBAR_START + 9;

    protected AbstractFilingCabinetMenu(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    /**
     * Adds the 8 folder slots (2 rows × 4 cols at x=29) and the player inventory/hotbar.
     *
     * @param folderContainer the container backing the folder slots
     * @param playerInv       the player's inventory
     * @param invY            top-y of the 3×9 player inventory rows
     * @param hotbarY         top-y of the hotbar row
     */
    protected void addAllSlots(Container folderContainer, Inventory playerInv, int invY, int hotbarY) {
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            addSlot(new Slot(folderContainer, i, 29 + col * 18, 44 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ManillaFolderItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    public abstract boolean isVoidExcess();

    public abstract void setVoidExcess(boolean on);

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index < FOLDER_SLOTS) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof ManillaFolderItem) {
                if (!moveItemStackTo(stack, 0, FOLDER_SLOTS, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
