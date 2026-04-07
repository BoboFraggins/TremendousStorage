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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for an Import Interface attachment on a Tube face.
 *
 * <p>No real item slots. Ghost filter state is tracked client-side in the screen and
 * synced via {@link net.bobofraggins.tremendousstorage.shared.network.SetImportExportFilterPacket}.
 *
 * <p>ContainerData slot 0: filter mode (0 = Accept, 1 = Reject).
 */
public class ImportInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int faceIndex;
    private final ContainerData data;

    /** Ghost filter slots displayed client-side (not real Slot objects). */
    private final ItemStack[] filterSlots = new ItemStack[9];

    private boolean rejectMode;

    /** Server-side constructor. */
    public ImportInterfaceMenu(int windowId, Inventory inv, BlockPos pos, int faceIndex, ContainerData data) {
        super(Registration.IMPORT_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.faceIndex = faceIndex;
        this.data = data;
        for (int i = 0; i < 9; i++) filterSlots[i] = ItemStack.EMPTY;
        addDataSlots(data);
    }

    /** Client-side constructor — reads pos and face from the network buffer. */
    public ImportInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), buf.readByte() & 0xFF, new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public boolean isRejectMode() {
        return data.get(0) != 0;
    }

    public ItemStack getFilterSlot(int slot) {
        return (slot >= 0 && slot < 9) ? filterSlots[slot] : ItemStack.EMPTY;
    }

    public void setFilterSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 9) filterSlots[slot] = stack;
    }

    public void applySync(java.util.List<ItemStack> slots, boolean reject) {
        for (int i = 0; i < 9 && i < slots.size(); i++) {
            filterSlots[i] =
                    slots.get(i) == null ? ItemStack.EMPTY : slots.get(i).copyWithCount(1);
        }
        rejectMode = reject;
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
            final int fi = faceIndex;
            ContainerData data = new ContainerData() {
                @Override
                public int get(int index) {
                    return index == 0 ? (be.getFilterMode(fi) ? 1 : 0) : 0;
                }

                @Override
                public void set(int index, int value) {}

                @Override
                public int getCount() {
                    return 1;
                }
            };
            ImportInterfaceMenu menu = new ImportInterfaceMenu(id, inv, pos, faceIndex, data);
            // Pre-populate filter slots from BE for initial open
            for (int s = 0; s < 9; s++) {
                menu.filterSlots[s] = be.getFilterSlot(faceIndex, s).copyWithCount(1);
                if (menu.filterSlots[s].isEmpty()) menu.filterSlots[s] = ItemStack.EMPTY;
            }
            return menu;
        }
    }
}
