package net.bobofraggins.intellistore.storage.tubeattachments;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for a Placer Interface attachment on a Tube face.
 *
 * <p>No real item slots. Ghost filter state (single slot) is tracked client-side and
 * synced via {@link net.bobofraggins.intellistore.shared.network.SetImportExportFilterPacket}.
 * No mode toggle — the single filter slot specifies the exact block to place.
 */
public class PlacerInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int faceIndex;

    /** Single ghost filter slot (the block type to place). */
    private ItemStack filterSlot = ItemStack.EMPTY;

    /** Server-side constructor. */
    public PlacerInterfaceMenu(int windowId, Inventory inv, BlockPos pos, int faceIndex) {
        super(Registration.PLACER_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.faceIndex = faceIndex;
    }

    /** Client-side constructor — reads pos and face from the network buffer. */
    public PlacerInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), buf.readByte() & 0xFF);
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public ItemStack getFilterSlot() {
        return filterSlot;
    }

    public void setFilterSlot(ItemStack stack) {
        filterSlot = stack;
    }

    public void applySync(ItemStack slot) {
        filterSlot = slot == null ? ItemStack.EMPTY : slot.copyWithCount(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // -------------------------------------------------------------------------
    // MenuProvider helper
    // -------------------------------------------------------------------------

    public static final class Provider implements MenuProvider {

        private final TubeBlockEntity be;
        private final BlockPos pos;
        private final int faceIndex;

        public Provider(TubeBlockEntity be, BlockPos pos, int faceIndex) {
            this.be = be;
            this.pos = pos;
            this.faceIndex = faceIndex;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.intellistore.placer_interface");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            PlacerInterfaceMenu menu = new PlacerInterfaceMenu(id, inv, pos, faceIndex);
            ItemStack slot = be.getFilterSlot(faceIndex, 0);
            menu.filterSlot = slot.isEmpty() ? ItemStack.EMPTY : slot.copyWithCount(1);
            return menu;
        }
    }
}
