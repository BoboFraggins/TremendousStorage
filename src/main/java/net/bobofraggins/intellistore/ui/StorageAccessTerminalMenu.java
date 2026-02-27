package net.bobofraggins.intellistore.ui;

import javax.annotation.Nullable;
import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.storagetransceiver.StorageAccessTerminalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Menu for the Storage Access Terminal.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0] craft result
 *   <li>[1..9] 3×3 crafting grid
 *   <li>[10..36] player main inventory (27 slots)
 *   <li>[37..45] player hotbar (9 slots)
 * </ul>
 *
 * <p>The network item list is NOT backed by real slots — it is rendered and interacted
 * with as a custom overlay in {@link StorageAccessTerminalScreen}, using packets.
 */
public class StorageAccessTerminalMenu extends AbstractContainerMenu {

    private static final int RESULT_SLOT    = 0;
    private static final int CRAFT_START    = 1;
    private static final int CRAFT_END      = 10;
    private static final int INV_START      = 10;
    private static final int INV_END        = 37;
    private static final int HOTBAR_START   = 37;
    private static final int HOTBAR_END     = 46;

    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    private final BlockPos satPos;
    @Nullable private final BlockPos niPos;

    // -------------------------------------------------------------------------
    // Server-side constructor (called by Provider)
    // -------------------------------------------------------------------------

    public StorageAccessTerminalMenu(int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos) {
        super(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), id);
        this.satPos = satPos;
        this.niPos = niPos;
        this.player = inv.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.access = ContainerLevelAccess.create(inv.player.level(), satPos);

        // Layout constants (image-relative, matching StorageAccessTerminalScreen)
        final int craftY  = 130; // top of crafting grid area
        final int invY    = 168; // top of player inventory
        final int hotbarY = invY + 3 * 18 + 4; // top of hotbar

        // Slot 0: craft result
        addSlot(new ResultSlot(inv.player, craftSlots, resultSlots, 0, 124, craftY + 9));

        // Slots 1-9: 3×3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, craftY + row * 18));
            }
        }

        // Slots 10-36: player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        // Slots 37-45: player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, hotbarY));
        }
    }

    // -------------------------------------------------------------------------
    // Client-side constructor (reads from FriendlyByteBuf)
    // -------------------------------------------------------------------------

    public StorageAccessTerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), buf.readBoolean() ? buf.readBlockPos() : null);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public BlockPos getSatPos() { return satPos; }

    @Nullable
    public BlockPos getNiPos() { return niPos; }

    public boolean hasNetwork() { return niPos != null; }

    // -------------------------------------------------------------------------
    // Crafting
    // -------------------------------------------------------------------------

    @Override
    public void slotsChanged(net.minecraft.world.Container inventory) {
        access.execute((level, pos) ->
                slotChangedCraftingGrid(this, level, player, craftSlots, resultSlots));
    }

    private static void slotChangedCraftingGrid(
            AbstractContainerMenu menu, Level level, Player player,
            CraftingContainer craftSlots, ResultContainer resultSlots) {
        if (level.isClientSide) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            if (resultSlots.setRecipeUsed(level, serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                menu.containerId, menu.incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return StorageAccessTerminalBlock.isStillValid(player.level(), satPos);
    }

    // -------------------------------------------------------------------------
    // Shift-click: between result/crafting/inventory; network insert via packet
    // -------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index == RESULT_SLOT) {
            // Shift-click result: move to inventory
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index >= INV_START && index < HOTBAR_END) {
            // Shift-click player slot: try crafting grid first, then other inventory rows
            if (!moveItemStackTo(stack, CRAFT_START, CRAFT_END, false)) {
                if (index < INV_END) {
                    if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false))
                        return ItemStack.EMPTY;
                } else {
                    if (!moveItemStackTo(stack, INV_START, INV_END, false))
                        return ItemStack.EMPTY;
                }
            }
        } else if (index >= CRAFT_START && index < CRAFT_END) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (index == RESULT_SLOT) player.drop(stack, false);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    // -------------------------------------------------------------------------
    // MenuProvider inner record
    // -------------------------------------------------------------------------

    public record Provider(BlockPos satPos, @Nullable BlockPos niPos) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.intellistore.storage_access_terminal");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new StorageAccessTerminalMenu(id, inv, satPos, niPos);
        }
    }
}
