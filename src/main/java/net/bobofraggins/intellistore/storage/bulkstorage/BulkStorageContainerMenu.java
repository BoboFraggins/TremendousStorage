package net.bobofraggins.intellistore.storage.bulkstorage;

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
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the Bulk Storage Container screen.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..26]  Player main inventory (27 slots)
 *   <li>[27..35] Player hotbar (9 slots)
 * </ul>
 *
 * <p>Shift-clicking a player slot inserts the item directly into the block entity server-side.
 * Grid extraction is handled by {@link net.bobofraggins.intellistore.shared.network.LocalStorageInteractPacket}.
 *
 * <p>A single {@link ContainerData} slot syncs the current priority ordinal from server to client.
 */
public class BulkStorageContainerMenu extends AbstractContainerMenu {

    // Player slot indices
    private static final int INV_START = 0;
    private static final int INV_END = 27;
    private static final int HOTBAR_START = 27;
    private static final int HOTBAR_END = 36;

    // Y positions matching PlayerInventoryPane rendered inside the Dialog
    // (TITLE_H=17) + (PriorityPane=33) + (LocalInventoryPane=77) = 127 from topPos
    private static final int PLAYER_INV_Y = 127;
    private static final int HOTBAR_Y = 185; // 127 + PlayerInventoryPane.HOTBAR_Y(58)

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public BulkStorageContainerMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.BULK_STORAGE_CONTAINER_MENU.get(), id);
        this.pos = pos;
        this.data = data;

        // Player main inventory (3 rows × 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    /** Client-side constructor. */
    public BulkStorageContainerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // Player slot → insert into bulk storage
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof BulkStorageContainerBlockEntity bulk) {
            long remainder = bulk.insert(stack, stack.getCount(), false);
            int moved = stack.getCount() - (int) remainder;
            if (moved > 0) stack.shrink(moved);
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
