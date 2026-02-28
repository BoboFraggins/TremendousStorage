package net.bobofraggins.intellistore.shared.ui;

import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Filing Cabinet block.
 *
 * <p>Slot layout (mirrors the Personal Filing Cabinet menu):
 * <ul>
 *   <li>[0..7]   8 folder slots (only accept ManillaFolderItem, stack size 1) at x=29, y=44
 *   <li>[8..34]  Player main inventory (27 slots) at y=118
 *   <li>[35..43] Player hotbar (9 slots) at y=176
 * </ul>
 *
 * <p>ContainerData[0]: voidExcess (0=OFF, 1=ON)
 * <p>ContainerData[1]: priority ordinal (0–4)
 */
public class FilingCabinetMenu extends AbstractContainerMenu {

    public static final int FOLDER_SLOTS = 8;
    private static final int INV_START = FOLDER_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final BlockPos pos;
    private final FilingCabinetBlockEntity be;
    private final ContainerData data;

    /** Server-side constructor — called from {@link FilingCabinetBlockEntity#createMenu}. */
    public FilingCabinetMenu(int syncId, Inventory playerInv, BlockPos pos, FilingCabinetBlockEntity be) {
        super(Registration.FILING_CABINET_MENU.get(), syncId);
        this.pos = pos;
        this.be = be;

        boolean[] voidExcessHolder = {be.isVoidExcess()};
        int[] priorityHolder = {be.getPriority().ordinal()};
        this.data = new SimpleContainerData(2) {
            @Override
            public int get(int index) {
                if (index == 0) return voidExcessHolder[0] ? 1 : 0;
                if (index == 1) return priorityHolder[0];
                return 0;
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) voidExcessHolder[0] = (value != 0);
                else if (index == 1) priorityHolder[0] = value;
            }
            @Override
            public int getCount() { return 2; }
        };

        // 8 folder slots (grid: 2 rows × 4 columns, at x=29, y=44)
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            addSlot(new Slot(be, i, 29 + col * 18, 44 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ManillaFolderItem;
                }
                @Override
                public int getMaxStackSize() { return 1; }
            });
        }

        // Player main inventory (below the priority row)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 176));
        }

        addDataSlots(data);
    }

    /** Client-side constructor. */
    public FilingCabinetMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        super(Registration.FILING_CABINET_MENU.get(), syncId);
        this.pos = buf.readBlockPos();
        this.be = null;
        this.data = new SimpleContainerData(2);

        net.minecraft.world.SimpleContainer dummy = new net.minecraft.world.SimpleContainer(FOLDER_SLOTS);

        for (int i = 0; i < FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            addSlot(new Slot(dummy, i, 29 + col * 18, 44 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ManillaFolderItem;
                }
                @Override
                public int getMaxStackSize() { return 1; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 176));
        }

        addDataSlots(data);
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isVoidExcess() {
        return data.get(0) != 0;
    }

    public void setVoidExcess(boolean on) {
        data.set(0, on ? 1 : 0);
    }

    public int getPriority() {
        return data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null) return true;
        return be.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index < FOLDER_SLOTS) {
            // Folder slot → player inventory
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else {
            // Player inventory/hotbar → try to place in a folder slot
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
