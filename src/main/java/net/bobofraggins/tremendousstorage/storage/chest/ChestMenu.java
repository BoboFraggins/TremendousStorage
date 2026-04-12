package net.bobofraggins.tremendousstorage.storage.chest;

import java.util.Optional;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
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
public class ChestMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;
    private final boolean hasCraftingUpgrade;
    private final boolean hasPullerUpgrade;

    // Crafting containers — non-null only when hasCraftingUpgrade is true
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final ContainerLevelAccess access;
    private final Player player;

    // Slot index ranges — adjusted per hasCraftingUpgrade
    private final int invStart;
    private final int invEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

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
        super(Registration.TREMENDOUS_CHEST_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.hasCraftingUpgrade = hasCraftingUpgrade;
        this.hasPullerUpgrade = hasPullerUpgrade;
        this.player = inv.player;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        int playerInvY = 49 + rows * 18;
        int hotbarY = playerInvY + 58;

        if (hasCraftingUpgrade) {
            this.craftSlots = new TransientCraftingContainer(this, 3, 3);
            this.resultSlots = new ResultContainer();

            // Slot 0: craft result
            addSlot(new ResultSlot(inv.player, craftSlots, resultSlots, 0, 120, playerInvY - 18 + 18));

            // Slots 1-9: 3×3 crafting grid (positioned above player inventory)
            int craftY = playerInvY - 18 - 3 * 18;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, craftY + row * 18));
                }
            }

            this.invStart = 10;
            this.invEnd = 37;
            this.hotbarStart = 37;
            this.hotbarEnd = 46;
        } else {
            this.craftSlots = null;
            this.resultSlots = null;
            this.invStart = 0;
            this.invEnd = 27;
            this.hotbarStart = 27;
            this.hotbarEnd = 36;
        }

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, hotbarY));
        }

        addDataSlots(data);
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
    }

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    public boolean hasPullerUpgrade() {
        return hasPullerUpgrade;
    }

    // -------------------------------------------------------------------------
    // Crafting
    // -------------------------------------------------------------------------

    @Override
    public void slotsChanged(net.minecraft.world.Container inventory) {
        if (!hasCraftingUpgrade) return;
        access.execute((level, p) -> updateCraftResult(this, level, player, craftSlots, resultSlots));
    }

    private static void updateCraftResult(
            AbstractContainerMenu menu,
            Level level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots) {
        if (level.isClientSide) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer sp = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            if (resultSlots.setRecipeUsed(level, sp, holder)) {
                ItemStack assembled = holder.value().assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            }
        }
        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                menu.containerId, menu.incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (hasCraftingUpgrade) {
            access.execute((level, p) -> clearContainer(player, craftSlots));
        }
        if (!player.level().isClientSide()) {
            if (player.level().getBlockEntity(pos) instanceof ChestBlockEntity be) {
                be.stopOpen(player);
            }
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (hasCraftingUpgrade && slot.container == resultSlots) return false;
        return super.canTakeItemForPickAll(stack, slot);
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

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (hasCraftingUpgrade && index == 0) {
            // Craft result → inventory
            if (!moveItemStackTo(stack, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, stack);
            return original;
        }

        if (hasCraftingUpgrade && index >= 1 && index < 10) {
            // Craft grid → inventory
            if (!moveItemStackTo(stack, invStart, hotbarEnd, false)) return ItemStack.EMPTY;
        } else if (index >= invStart && index < hotbarEnd) {
            // Player slot → bulk storage
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
}
