package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for a Breaker Interface attachment on a Tube face.
 *
 * <p>No real item slots. Ghost filter state (single slot) is tracked client-side and
 * synced via {@link net.bobofraggins.intellistore.network.SetImportExportFilterPacket}.
 * ContainerData slot 0: silkTouch (0 = off, 1 = on).
 */
public class BreakerInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int faceIndex;
    private final ContainerData data;

    /** Single ghost filter slot (the block type to break). */
    private ItemStack filterSlot = ItemStack.EMPTY;

    /** Server-side constructor. */
    public BreakerInterfaceMenu(int windowId, Inventory inv, BlockPos pos, int faceIndex, ContainerData data) {
        super(Registration.BREAKER_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.faceIndex = faceIndex;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor — reads pos and face from the network buffer. */
    public BreakerInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), buf.readByte() & 0xFF, new SimpleContainerData(1));
    }

    public BlockPos getPos() { return pos; }
    public int getFaceIndex() { return faceIndex; }

    public boolean isSilkTouch() { return data.get(0) != 0; }

    public ItemStack getFilterSlot() { return filterSlot; }
    public void setFilterSlot(ItemStack stack) { filterSlot = stack; }

    public void applySync(ItemStack slot) {
        filterSlot = slot == null ? ItemStack.EMPTY : slot.copyWithCount(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }

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
            return Component.translatable("screen.intellistore.breaker_interface");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            final int fi = faceIndex;
            ContainerData data = new ContainerData() {
                @Override public int get(int index) { return index == 0 ? (be.getSilkTouch(fi) ? 1 : 0) : 0; }
                @Override public void set(int index, int value) {}
                @Override public int getCount() { return 1; }
            };
            BreakerInterfaceMenu menu = new BreakerInterfaceMenu(id, inv, pos, faceIndex, data);
            ItemStack slot = be.getFilterSlot(faceIndex, 0);
            menu.filterSlot = slot.isEmpty() ? ItemStack.EMPTY : slot.copyWithCount(1);
            return menu;
        }
    }
}
