package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Picnic Basket item-form screen.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..26]  Player main inventory (27 slots)
 *   <li>[27..35] Player hotbar (9 slots)
 * </ul>
 *
 * <p>The basket's stored items are NOT menu slots — they are read directly from the item's
 * {@link net.minecraft.core.component.DataComponents#BLOCK_ENTITY_DATA} by
 * {@link PicnicBasketItemScreen} and interactions are handled by {@link PicnicBasketItemInteractPacket}.
 */
public class PicnicBasketItemMenu extends AbstractContainerMenu {

    private final int slotType;
    private final int slotIndex;
    private final String slotId;

    /** Server-side constructor. Uses the default row count. */
    public PicnicBasketItemMenu(int syncId, Inventory playerInv, int slotType, int slotIndex, String slotId) {
        this(syncId, playerInv, slotType, slotIndex, slotId, TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Client-side constructor (reads slot location from the buffer). */
    public PicnicBasketItemMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(
                syncId,
                playerInv,
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                TremendousStorageClientConfig.getVisibleRowsSafe());
    }

    private PicnicBasketItemMenu(
            int syncId, Inventory playerInv, int slotType, int slotIndex, String slotId, int rows) {
        super(Registration.PICNIC_BASKET_ITEM_MENU.get(), syncId);
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.slotId = slotId;

        int playerInvY = 49 + rows * 18;
        int hotbarY = playerInvY + 58;

        // Player main inventory (slots 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 20 + col * 18, playerInvY + row * 18));
            }
        }
        // Player hotbar (slots 0-8)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 20 + col * 18, hotbarY));
        }
    }

    public int getSlotType() {
        return slotType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getSlotId() {
        return slotId;
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

        // All slots are player slots → try inserting into the basket
        ItemStack basketStack = PicnicBasketItemUtils.getBasketStack(player, slotType, slotIndex, slotId);
        if (!basketStack.isEmpty()) {
            long inserted = PicnicBasketItemUtils.insertIntoBasketItem(
                    player.level().registryAccess(), basketStack, stack, stack.getCount());
            if (inserted > 0) {
                PicnicBasketItemUtils.setBasketStack(player, basketStack, slotType, slotIndex, slotId);
                stack.shrink((int) inserted);
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
