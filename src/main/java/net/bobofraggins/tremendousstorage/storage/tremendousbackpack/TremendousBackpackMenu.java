package net.bobofraggins.tremendousstorage.storage.tremendousbackpack;

import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Tremendous Backpack screen.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..26]  Player main inventory (27 slots)
 *   <li>[27..35] Player hotbar (9 slots)
 * </ul>
 *
 * <p>Shift-clicking a player slot inserts the item into the backpack server-side.
 * Grid extraction is handled by {@link net.bobofraggins.tremendousstorage.shared.network.TremendousBackpackInteractPacket}.
 *
 * <p>A single {@link ContainerData} slot syncs the current priority ordinal from server to client.
 */
public class TremendousBackpackMenu extends AbstractContainerMenu {

    private final int slotType;
    private final int slotIndex;
    private final String slotId;
    private final ContainerData data;

    /** Server-side constructor. Uses default row count. */
    public TremendousBackpackMenu(
            int syncId, Inventory playerInv, int slotType, int slotIndex, String slotId, ContainerData data) {
        this(
                syncId,
                playerInv,
                slotType,
                slotIndex,
                slotId,
                data,
                TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Client-side constructor. Reads slot location from the buffer. */
    public TremendousBackpackMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(
                syncId,
                playerInv,
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                new SimpleContainerData(1),
                TremendousStorageClientConfig.getVisibleRowsSafe());
    }

    /**
     * Internal constructor. Computes player inventory Y from row count.
     *
     * <p>Layout: title(17) + blank(7) + invPane(rows×18+5) + blank(20) + playerInv
     * → playerInvY = 49 + rows×18.
     */
    private TremendousBackpackMenu(
            int syncId, Inventory playerInv, int slotType, int slotIndex, String slotId, ContainerData data, int rows) {
        super(Registration.TREMENDOUS_BACKPACK_MENU.get(), syncId);
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.slotId = slotId;
        this.data = data;

        int playerInvY = 49 + rows * 18;
        int hotbarY = playerInvY + 58;

        // Player main inventory (3 rows × 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }

        addDataSlots(data);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public int getSlotType() {
        return slotType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getSlotId() {
        return slotId;
    }

    public int getPriority() {
        return data.get(0);
    }

    /** Called from {@link net.bobofraggins.tremendousstorage.shared.network.SetTremendousBackpackPriorityPacket} to push new priority to client. */
    public void setPriorityInData(int ordinal) {
        data.set(0, ordinal);
    }

    // -------------------------------------------------------------------------
    // Menu lifecycle
    // -------------------------------------------------------------------------

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    // -------------------------------------------------------------------------
    // Shift-click: insert player item into backpack
    // -------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        ItemStack backpackStack = TremendousBackpackItem.getBackpackStack(player, slotType, slotIndex, slotId);
        if (backpackStack.isEmpty()) return ItemStack.EMPTY;

        TremendousBackpackContents current = backpackStack.getOrDefault(
                Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
        Object[] result = current.withInserted(stack, stack.getCount());
        long remainder = (long) result[0];
        TremendousBackpackContents updated = (TremendousBackpackContents) result[1];
        int moved = (int) (stack.getCount() - remainder);

        if (moved > 0) {
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
            TremendousBackpackItem.setBackpackStack(player, backpackStack, slotType, slotIndex, slotId);
            stack.shrink(moved);
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (moved == 0) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
