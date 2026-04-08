package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Recycling Bin. Contains one void slot where any item placed into
 * it is immediately destroyed. Player inventory slots are shown below.
 */
public class RecyclingBinMenu extends AbstractContainerMenu {

    static final int VOID_SLOT_X = 80;
    static final int VOID_SLOT_Y = 20;
    private static final int INV_START_X = 8;
    private static final int INV_Y = 51;
    private static final int HOTBAR_Y = 109;

    private final BlockPos pos;

    /** Server-side constructor. */
    public RecyclingBinMenu(int id, Inventory inv, BlockPos pos) {
        super(Registration.RECYCLING_BIN_MENU.get(), id);
        this.pos = pos;

        // Slot 0: void slot — anything placed here is instantly destroyed
        addSlot(new Slot(new VoidContainer(), 0, VOID_SLOT_X, VOID_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public void setChanged() {
                // Destroy the item immediately
                container.setItem(0, ItemStack.EMPTY);
            }
        });

        // Player inventory (slots 1–27)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_START_X + col * 18, INV_Y + row * 18));
            }
        }
        // Hotbar (slots 28–36)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_START_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side constructor — reads BlockPos from network buffer. */
    public RecyclingBinMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            if (player.level().getBlockEntity(pos) instanceof RecyclingBinBlockEntity be) {
                be.stopOpen(player);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();

        if (slotIndex == 0) {
            // Void slot → player inventory — item was already destroyed, nothing to move
            slot.setChanged();
            return ItemStack.EMPTY;
        }

        // Player inventory → void slot (destroy the item)
        if (slotIndex >= 1) {
            slot.set(ItemStack.EMPTY);
        }

        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // Inner: minimal single-slot container whose setItem notifies the void slot
    // -------------------------------------------------------------------------

    private static class VoidContainer implements net.minecraft.world.Container {
        private ItemStack item = ItemStack.EMPTY;

        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return true; }
        @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }

        @Override
        public void setItem(int slot, ItemStack stack) {
            // Destroy by simply not storing it
            item = ItemStack.EMPTY;
        }

        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() {}
    }
}
