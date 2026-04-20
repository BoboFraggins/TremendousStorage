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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for a Storage Interface attachment on a Tube face.
 *
 * <p>No item slots. Carries one server→client data value:
 * <ul>
 *   <li>slot 0: priority ordinal (0–4)
 * </ul>
 *
 * <p>Also carries the block position and face index so packets know which attachment to update.
 */
public class StorageInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int faceIndex;
    private final ContainerData data;

    // Player inventory slot Y offsets (matching PlayerInventoryPane with default leftMargin=20).
    // PriorityPane height=35 sits below Dialog.TITLE_H=17, so PlayerInventoryPane starts at y=52.
    private static final int INV_X = 20;
    private static final int INV_Y = 52;
    private static final int HOTBAR_Y = INV_Y + 3 * 18 + 4; // 110

    /** Server-side constructor. */
    public StorageInterfaceMenu(int windowId, Inventory inv, BlockPos pos, int faceIndex, ContainerData data) {
        super(Registration.STORAGE_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.faceIndex = faceIndex;
        this.data = data;
        addDataSlots(data);
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

    /** Client-side constructor — reads pos and face from the network buffer. */
    public StorageInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), buf.readByte() & 0xFF, new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public int getPriority() {
        return data.get(0);
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
    // MenuProvider helper — used by TubeBlock.useWithoutItem
    // -------------------------------------------------------------------------

    /**
     * Simple {@link MenuProvider} that wraps a tube's attachment data for {@code player.openMenu}.
     */
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
            return Component.translatable("screen.tremendousstorage.storage_interface");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            ContainerData data = new ContainerData() {
                @Override
                public int get(int index) {
                    return index == 0 ? be.getAttachmentPriority(faceIndex).ordinal() : 0;
                }

                @Override
                public void set(int index, int value) {}

                @Override
                public int getCount() {
                    return 1;
                }
            };
            return new StorageInterfaceMenu(id, inv, pos, faceIndex, data);
        }
    }
}
