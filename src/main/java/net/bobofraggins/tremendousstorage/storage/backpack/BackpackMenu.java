package net.bobofraggins.tremendousstorage.storage.backpack;

import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.crafting.AbstractCraftingUpgradeMenu;
import net.bobofraggins.tremendousstorage.shared.crafting.CraftingRefillSource;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Tremendous Backpack screen.
 *
 * <p>Without crafting upgrade — slot layout:
 * <ul>
 *   <li>[0..26]  Player main inventory (27 slots)
 *   <li>[27..35] Player hotbar (9 slots)
 * </ul>
 *
 * <p>With crafting upgrade — slot layout:
 * <ul>
 *   <li>[0]      Craft result
 *   <li>[1..9]   3×3 crafting grid
 *   <li>[10..36] Player main inventory (27 slots)
 *   <li>[37..45] Player hotbar (9 slots)
 * </ul>
 */
public class BackpackMenu extends AbstractCraftingUpgradeMenu {

    private final int slotType;
    private final int slotIndex;
    private final String slotId;
    private final ContainerData data;

    /** Server-side constructor. Uses default row count. */
    public BackpackMenu(
            int syncId,
            Inventory playerInv,
            int slotType,
            int slotIndex,
            String slotId,
            ContainerData data,
            boolean hasCraftingUpgrade) {
        this(
                Registration.TREMENDOUS_BACKPACK_MENU.get(),
                syncId,
                playerInv,
                slotType,
                slotIndex,
                slotId,
                data,
                hasCraftingUpgrade,
                TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Client-side constructor. Reads slot location and crafting flag from the buffer. */
    public BackpackMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(
                Registration.TREMENDOUS_BACKPACK_MENU.get(),
                syncId,
                playerInv,
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                new SimpleContainerData(1),
                buf.readBoolean(),
                TremendousStorageClientConfig.getVisibleRowsSafe());
    }

    /**
     * Protected constructor that allows subclasses (e.g. ender backpack) to supply their own
     * {@link MenuType} while reusing all slot-layout logic.
     */
    protected BackpackMenu(
            MenuType<?> menuType,
            int syncId,
            Inventory playerInv,
            int slotType,
            int slotIndex,
            String slotId,
            ContainerData data,
            boolean hasCraftingUpgrade,
            int rows) {
        super(
                menuType,
                syncId,
                playerInv.player,
                hasCraftingUpgrade,
                ContainerLevelAccess.create(playerInv.player.level(), playerInv.player.blockPosition()),
                hasCraftingUpgrade ? 10 : 0,
                hasCraftingUpgrade ? 37 : 27,
                hasCraftingUpgrade ? 37 : 27,
                hasCraftingUpgrade ? 46 : 36);
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.slotId = slotId;
        this.data = data;

        int playerInvY;
        if (hasCraftingUpgrade) {
            int craftY = 29 + rows * 18; // title(17) + blank(7) + inventoryPane gap(5) = 29
            addCraftingSlots(120, craftY + 18, 30, craftY, 18);
            playerInvY = craftY + 3 * 18 + 4;
        } else {
            playerInvY = 49 + rows * 18; // title(17) + blank(7) + inventoryPane gap(5) + blank(20) = 49
        }

        int hotbarY = playerInvY + 58;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 20 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 20 + col * 18, hotbarY));
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

    /** Called from {@link net.bobofraggins.tremendousstorage.shared.network.SetBackpackPriorityPacket}. */
    public void setPriorityInData(int ordinal) {
        data.set(0, ordinal);
    }

    // -------------------------------------------------------------------------
    // Crafting — primary source
    // -------------------------------------------------------------------------

    @Override
    protected CraftingRefillSource createPrimarySource(Player player) {
        return new BackpackRefillSource(player);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCraftingMenuRemoved(Player player) {
        onMenuRemoved(player);
    }

    /** Hook called after the menu is closed. Subclasses (e.g. Ender Backpack) override this to sync contents to storage. */
    protected void onMenuRemoved(Player player) {}

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // -------------------------------------------------------------------------
    // Shift-click
    // -------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        if (hasCraftingUpgrade && index == 0) {
            return quickMoveResult(player, slot);
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (hasCraftingUpgrade && index >= 1 && index < 10) {
            // Craft grid → inventory
            if (!moveItemStackTo(stack, invStart, hotbarEnd, false)) return ItemStack.EMPTY;
        } else if (index >= invStart && index < hotbarEnd) {
            // Player slot → backpack. Skip client prediction: client can't reliably replicate
            // item-data-component insertion, which would cause a flicker.
            if (player.level().isClientSide()) return ItemStack.EMPTY;
            ItemStack backpackStack = BackpackItem.getBackpackStack(player, slotType, slotIndex, slotId);
            if (backpackStack.isEmpty()) return ItemStack.EMPTY;

            BackpackContents current =
                    backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
            Object[] result = current.withInserted(stack, stack.getCount());
            long remainder = (long) result[0];
            BackpackContents updated = (BackpackContents) result[1];
            int moved = (int) (stack.getCount() - remainder);

            if (moved > 0) {
                backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
                BackpackItem.setBackpackStack(player, backpackStack, slotType, slotIndex, slotId);
                stack.shrink(moved);
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    // -------------------------------------------------------------------------
    // Clear-grid — return to backpack storage
    // -------------------------------------------------------------------------

    @Override
    protected ItemStack returnToStorage(Player player, ItemStack stack) {
        ItemStack backpackStack = BackpackItem.getBackpackStack(player, slotType, slotIndex, slotId);
        if (backpackStack.isEmpty()) return stack;
        BackpackContents current =
                backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
        Object[] result = current.withInserted(stack, stack.getCount());
        long remainder = (long) result[0];
        BackpackContents updated = (BackpackContents) result[1];
        int moved = (int) (stack.getCount() - remainder);
        if (moved > 0) {
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
            BackpackItem.setBackpackStack(player, backpackStack, slotType, slotIndex, slotId);
        }
        return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount((int) remainder);
    }

    // -------------------------------------------------------------------------
    // Backpack refill source
    // -------------------------------------------------------------------------

    private class BackpackRefillSource implements CraftingRefillSource {
        private BackpackContents contents;
        private final ItemStack backpackStack;
        private boolean dirty = false;

        BackpackRefillSource(Player player) {
            this.backpackStack = BackpackItem.getBackpackStack(player, slotType, slotIndex, slotId);
            this.contents = backpackStack.isEmpty()
                    ? BackpackContents.EMPTY
                    : backpackStack.getOrDefault(
                            Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
        }

        @Override
        public ItemStack extractOne(ItemStack template) {
            if (backpackStack.isEmpty()) return ItemStack.EMPTY;
            for (int t = 0; t < contents.typeCount(); t++) {
                if (ItemStack.isSameItemSameComponents(contents.getType(t), template)) {
                    Object[] result = contents.withExtracted(t, 1);
                    ItemStack extracted = (ItemStack) result[0];
                    if (!extracted.isEmpty()) {
                        contents = (BackpackContents) result[1];
                        dirty = true;
                        return extracted;
                    }
                    break;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void returnOne(ItemStack stack) {
            if (backpackStack.isEmpty()) return;
            Object[] ins = contents.withInserted(stack, 1);
            contents = (BackpackContents) ins[1];
            dirty = true;
        }

        @Override
        public void commit(Player player) {
            if (!dirty || backpackStack.isEmpty()) return;
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
            BackpackItem.setBackpackStack(player, backpackStack, slotType, slotIndex, slotId);
        }
    }
}
