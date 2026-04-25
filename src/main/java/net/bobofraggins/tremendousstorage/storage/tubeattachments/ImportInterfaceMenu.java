package net.bobofraggins.tremendousstorage.storage.tubeattachments;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for an Import Interface attachment on a Tube face.
 *
 * <p>No real item slots. Ghost filter state is tracked client-side in the screen and
 * synced via {@link net.bobofraggins.tremendousstorage.shared.network.SetImportExportFilterPacket}.
 */
public class ImportInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int faceIndex;

    /** Ghost filter slots displayed client-side (not real Slot objects). */
    private final ItemStack[] filterSlots = new ItemStack[9];

    // Player inventory slot offsets — leftMargin=7 keeps 9 columns inside the 176 px dialog.
    // blankPane(176, 76) sits below Dialog.TITLE_H=17, so PlayerInventoryPane starts at y=93.
    static final int INV_X = 7;
    private static final int INV_Y = 93;
    private static final int HOTBAR_Y = INV_Y + 3 * 18 + 4; // 151

    /** Server-side constructor. */
    public ImportInterfaceMenu(int windowId, Inventory inv, BlockPos pos, int faceIndex) {
        super(Registration.IMPORT_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.faceIndex = faceIndex;
        for (int i = 0; i < 9; i++) filterSlots[i] = ItemStack.EMPTY;
        addPlayerInventory(inv);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side constructor — reads pos, face, and initial filter slots from the network buffer. */
    public ImportInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), buf.readByte() & 0xFF);
        net.minecraft.network.RegistryFriendlyByteBuf regBuf = (net.minecraft.network.RegistryFriendlyByteBuf) buf;
        for (int s = 0; s < 9; s++) {
            filterSlots[s] = ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf);
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public ItemStack getFilterSlot(int slot) {
        return (slot >= 0 && slot < 9) ? filterSlots[slot] : ItemStack.EMPTY;
    }

    public void setFilterSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 9) filterSlots[slot] = stack;
    }

    public void applySync(java.util.List<ItemStack> slots) {
        for (int i = 0; i < 9 && i < slots.size(); i++) {
            filterSlots[i] =
                    slots.get(i) == null ? ItemStack.EMPTY : slots.get(i).copyWithCount(1);
        }
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
            return Component.translatable("screen.tremendousstorage.import_interface");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            ImportInterfaceMenu menu = new ImportInterfaceMenu(id, inv, pos, faceIndex);
            for (int s = 0; s < 9; s++) {
                menu.filterSlots[s] = be.getFilterSlot(faceIndex, s).copyWithCount(1);
                if (menu.filterSlots[s].isEmpty()) menu.filterSlots[s] = ItemStack.EMPTY;
            }
            return menu;
        }
    }
}
