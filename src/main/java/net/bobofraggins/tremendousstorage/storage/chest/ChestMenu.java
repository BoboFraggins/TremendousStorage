package net.bobofraggins.tremendousstorage.storage.chest;

import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.crafting.AbstractCraftingUpgradeMenu;
import net.bobofraggins.tremendousstorage.shared.crafting.CraftingRefillSource;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the Tremendous Chest screen.
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
public class ChestMenu extends AbstractCraftingUpgradeMenu {

    private final BlockPos pos;
    private final ContainerData data;
    private final boolean hasPullerUpgrade;

    /** Server-side constructor. Uses the default row count. */
    public ChestMenu(int id, Inventory inv, BlockPos pos, ContainerData data, boolean hasCraftingUpgrade) {
        this(id, inv, pos, data, hasCraftingUpgrade, false, TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Server-side constructor with puller upgrade flag. */
    public ChestMenu(
            int id,
            Inventory inv,
            BlockPos pos,
            ContainerData data,
            boolean hasCraftingUpgrade,
            boolean hasPullerUpgrade) {
        this(
                id,
                inv,
                pos,
                data,
                hasCraftingUpgrade,
                hasPullerUpgrade,
                TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Client-side constructor. Reads slot location, crafting flag, and puller flag from the buffer. */
    public ChestMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(
                id,
                inv,
                buf.readBlockPos(),
                new SimpleContainerData(1),
                buf.readBoolean(),
                buf.readBoolean(),
                TremendousStorageClientConfig.getVisibleRowsSafe());
    }

    private ChestMenu(
            int id,
            Inventory inv,
            BlockPos pos,
            ContainerData data,
            boolean hasCraftingUpgrade,
            boolean hasPullerUpgrade,
            int rows) {
        super(
                Registration.TREMENDOUS_CHEST_MENU.get(),
                id,
                inv.player,
                hasCraftingUpgrade,
                ContainerLevelAccess.create(inv.player.level(), pos),
                hasCraftingUpgrade ? 10 : 0,
                hasCraftingUpgrade ? 37 : 27,
                hasCraftingUpgrade ? 37 : 27,
                hasCraftingUpgrade ? 46 : 36);
        this.pos = pos;
        this.data = data;
        this.hasPullerUpgrade = hasPullerUpgrade;

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
                addSlot(new Slot(inv, col + row * 9 + 9, 20 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 20 + col * 18, hotbarY));
        }
        addDataSlots(data);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
    }

    public boolean hasPullerUpgrade() {
        return hasPullerUpgrade;
    }

    // -------------------------------------------------------------------------
    // Crafting — primary source
    // -------------------------------------------------------------------------

    @Override
    protected CraftingRefillSource createPrimarySource(Player player) {
        return new ChestRefillSource();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCraftingMenuRemoved(Player player) {
        if (!player.level().isClientSide()) {
            if (player.level().getBlockEntity(pos) instanceof ChestBlockEntity be) {
                be.stopOpen(player);
            }
        }
    }

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
            // Player slot → bulk storage. Skip client prediction: client can't replicate
            // server-side bulk insertion, which would cause a flicker.
            if (player.level().isClientSide()) return ItemStack.EMPTY;
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof ChestBlockEntity bulk) {
                long remainder = bulk.insert(stack, stack.getCount(), false);
                int moved = stack.getCount() - (int) remainder;
                if (moved > 0) stack.shrink(moved);
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    // -------------------------------------------------------------------------
    // Chest refill source
    // -------------------------------------------------------------------------

    private class ChestRefillSource implements CraftingRefillSource {
        @Override
        public ItemStack extractOne(ItemStack template) {
            if (!(player.level().getBlockEntity(pos) instanceof ChestBlockEntity be)) return ItemStack.EMPTY;
            for (int t = 0; t < be.typeCount(); t++) {
                if (ItemStack.isSameItemSameComponents(be.getType(t), template)) {
                    return be.extract(t, 1, false);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void returnOne(ItemStack stack) {
            if (!(player.level().getBlockEntity(pos) instanceof ChestBlockEntity be)) return;
            be.insert(stack, 1, false);
        }
    }
}
