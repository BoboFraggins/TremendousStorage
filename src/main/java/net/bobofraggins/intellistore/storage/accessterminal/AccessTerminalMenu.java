package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.Optional;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
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
import net.neoforged.neoforge.items.IItemHandler;

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
 * with as a custom overlay in {@link AccessTerminalScreen}, using packets.
 */
public class AccessTerminalMenu extends AbstractContainerMenu {

    private static final int RESULT_SLOT = 0;
    private static final int CRAFT_START = 1;
    private static final int CRAFT_END = 10;
    private static final int INV_START = 10;
    private static final int INV_END = 37;
    private static final int HOTBAR_START = 37;
    private static final int HOTBAR_END = 46;

    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    private final BlockPos satPos;

    @Nullable
    private final BlockPos niPos;

    // -------------------------------------------------------------------------
    // Server-side constructor (called by Provider)
    // -------------------------------------------------------------------------

    public AccessTerminalMenu(int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos) {
        super(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), id);
        this.satPos = satPos;
        this.niPos = niPos;
        this.player = inv.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.access = ContainerLevelAccess.create(inv.player.level(), satPos);

        // Layout constants (image-relative, must match AccessTerminalScreen)
        // GRID_Y=18, GRID_H=72, CRAFT_GAP=4 → craftY = 18+72+4 = 94
        // CRAFT_GRID_X=30, RESULT_X=110, RESULT_Y=craftY+18=112
        // INV_Y = craftY + 3*18 + 4 = 152, HOTBAR_Y = INV_Y + 3*18 + 4 = 210
        final int craftY = 94;
        final int invY = 152;
        final int hotbarY = 210;

        // Slot 0: craft result
        addSlot(new ResultSlot(inv.player, craftSlots, resultSlots, 0, 110, craftY + 18));

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

    public AccessTerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), buf.readBoolean() ? buf.readBlockPos() : null);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public BlockPos getSatPos() {
        return satPos;
    }

    @Nullable
    public BlockPos getNiPos() {
        return niPos;
    }

    public boolean hasNetwork() {
        return niPos != null;
    }

    // -------------------------------------------------------------------------
    // Crafting
    // -------------------------------------------------------------------------

    @Override
    public void slotsChanged(net.minecraft.world.Container inventory) {
        access.execute((level, pos) -> slotChangedCraftingGrid(this, level, player, craftSlots, resultSlots));
    }

    private static void slotChangedCraftingGrid(
            AbstractContainerMenu menu,
            Level level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots) {
        if (level.isClientSide) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
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
        return AccessTerminalBlock.isStillValid(player.level(), satPos);
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
            // Snapshot the craft grid before ingredients are consumed by onTake
            ItemStack[] gridSnapshot = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                gridSnapshot[i] = craftSlots.getItem(i).copy();
            }

            // Shift-click result: move to inventory
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            player.drop(stack, false);

            // Refill depleted craft slots from the network
            refillCraftGridFromNetwork(player, gridSnapshot);

            return copy;
        } else if (index >= INV_START && index < HOTBAR_END) {
            // Shift-click player slot: try network first, then swap between inv/hotbar
            if (hasNetwork() && !player.level().isClientSide()) {
                if (player.level().getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
                    IItemHandler handler = ni.getItemHandler();
                    if (handler != null) {
                        ItemStack remainder = handler.insertItem(0, stack, false);
                        slot.set(remainder);
                        slotsChanged(craftSlots);
                        return copy;
                    }
                }
            }
            // Fallback: swap between main inv and hotbar
            if (index < INV_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
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
    // Network auto-refill
    // -------------------------------------------------------------------------

    /**
     * After a craft result is taken, refill any depleted craft grid slot from the network.
     *
     * <p>For each of the 9 grid slots: if the slot is now empty but the pre-craft snapshot
     * had an item there, pull one stack of that item type from the network and place it.
     * Triggers {@link #slotsChanged} so the result slot updates immediately.
     *
     * @param player       the crafting player (used to get the server level)
     * @param gridSnapshot copies of each craft slot taken just before {@code onTake} ran
     */
    private void refillCraftGridFromNetwork(Player player, ItemStack[] gridSnapshot) {
        if (!hasNetwork() || player.level().isClientSide()) return;
        if (!(player.level().getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        IItemHandler handler = ni.getItemHandler();
        if (handler == null) return;

        boolean anyRefilled = false;
        for (int i = 0; i < 9; i++) {
            ItemStack snap = gridSnapshot[i];
            if (snap.isEmpty()) continue; // slot was empty before craft, nothing to refill

            ItemStack current = craftSlots.getItem(i);

            if (current.isEmpty()) {
                // Normal case: ingredient was fully consumed — pull a fresh stack from network
                anyRefilled |= extractFromNetworkIntoSlot(handler, snap, i);
            } else if (snap.hasCraftingRemainingItem()) {
                // Container item case (e.g. lava bucket → empty bucket):
                // onTake placed the remainder (empty bucket) back in the slot.
                // If that remainder matches what's there now, swap it for a full one.
                ItemStack remainder = snap.getCraftingRemainingItem();
                if (!remainder.isEmpty() && ItemStack.isSameItemSameComponents(current, remainder)) {
                    // Try to extract the full item (e.g. lava bucket) from the network
                    ItemStack needed = snap.copyWithCount(1);
                    ItemStack extracted = tryExtractFromNetwork(handler, needed);
                    if (!extracted.isEmpty()) {
                        // Return the remainder (empty bucket) to the network
                        handler.insertItem(0, current.copyWithCount(1), false);
                        craftSlots.setItem(i, extracted);
                        anyRefilled = true;
                    }
                }
            }
        }

        if (anyRefilled) {
            slotsChanged(craftSlots);
        }
    }

    /** Extracts one stack of {@code template} from the network and places it in craft slot {@code gridIndex}. */
    private boolean extractFromNetworkIntoSlot(IItemHandler handler, ItemStack template, int gridIndex) {
        ItemStack extracted = tryExtractFromNetwork(handler, template.copyWithCount(1));
        if (!extracted.isEmpty()) {
            craftSlots.setItem(gridIndex, extracted);
            return true;
        }
        return false;
    }

    /**
     * Finds the first network slot containing an item matching {@code template} and extracts
     * up to one full stack from it. Returns the extracted stack, or {@link ItemStack#EMPTY}.
     */
    private static ItemStack tryExtractFromNetwork(IItemHandler handler, ItemStack template) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), template)) continue;
            ItemStack extracted = handler.extractItem(slot, template.getMaxStackSize(), false);
            if (!extracted.isEmpty()) return extracted;
        }
        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // MenuProvider inner record
    // -------------------------------------------------------------------------

    public record Provider(BlockPos satPos, @Nullable BlockPos niPos) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.intellistore.access_terminal");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new AccessTerminalMenu(id, inv, satPos, niPos);
        }
    }
}
