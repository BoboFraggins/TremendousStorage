package net.bobofraggins.tremendousstorage.storage.accessterminal;

import java.util.Optional;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.network.RequestSatContentsPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Menu for the Storage Access Terminal.
 *
 * <p>Slot indices:
 * <ul>
 *   <li>[0] craft result
 *   <li>[1..9] 3×3 crafting grid
 *   <li>[10..36] player main inventory (27 slots)
 *   <li>[37..45] player hotbar (9 slots)
 * </ul>
 *
 * <p>All pixel coordinates come from {@link AccessTerminalLayout}.
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

    /** Last cache revision we sent to this player; -1 = never sent. */
    private long lastSentRevision = -1;

    // -------------------------------------------------------------------------
    // Server-side constructor (called by Provider)
    // -------------------------------------------------------------------------

    /**
     * Server-side constructor. Uses the default row count (slot positions are unused server-side).
     */
    public AccessTerminalMenu(int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos) {
        this(id, inv, satPos, niPos, TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Client-side constructor. Reads the configured row count for the current GUI scale. */
    public AccessTerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(
                id,
                inv,
                buf.readBlockPos(),
                buf.readBoolean() ? buf.readBlockPos() : null,
                TremendousStorageClientConfig.getVisibleRowsSafe());
    }

    /**
     * Internal constructor that computes all slot Y positions from the given network grid row
     * count.
     *
     * <p>Layout: title(17) + networkPane(rows×18+5) + craftingPane(58) + playerInv
     * → craftingY = 22 + rows×18; fixed 58-px offsets to player inv and hotbar.
     */
    private AccessTerminalMenu(int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos, int rows) {
        super(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), id);
        this.satPos = satPos;
        this.niPos = niPos;
        this.player = inv.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.access = ContainerLevelAccess.create(inv.player.level(), satPos);

        final int S = AccessTerminalLayout.SLOT_SIZE;
        // craftingY shifts with the network pane height; offsets below it are fixed.
        int craftingY = AccessTerminalLayout.TITLE_H + rows * S + 5; // 22 + rows*18
        int playerInvY = craftingY + (AccessTerminalLayout.PLAYER_INV_Y - AccessTerminalLayout.CRAFTING_Y); // +58
        int hotbarY = playerInvY + (AccessTerminalLayout.HOTBAR_Y - AccessTerminalLayout.PLAYER_INV_Y); // +58

        // Slot 0: craft result (one slot-row below craftingY)
        addSlot(new ResultSlot(
                inv.player, craftSlots, resultSlots, 0, AccessTerminalLayout.CRAFTING_RESULT_X, craftingY + S));

        // Slots 1-9: 3×3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(
                        craftSlots,
                        col + row * 3,
                        AccessTerminalLayout.CRAFTING_GRID_X + col * S,
                        craftingY + row * S));
            }
        }

        // Slots 10-36: player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        inv, col + row * 9 + 9, AccessTerminalLayout.PLAYER_INV_X + col * S, playerInvY + row * S));
            }
        }

        // Slots 37-45: player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, AccessTerminalLayout.HOTBAR_X + col * S, hotbarY));
        }
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
                        // Refresh the client's network grid
                        KeyCounter inventory = ni.getCachedInventory();
                        if (inventory != null && player instanceof ServerPlayer sp) {
                            PacketDistributor.sendToPlayer(sp, RequestSatContentsPacket.buildContentsPacket(inventory));
                        }
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
    // Recipe viewer fill (EMI / REI)
    // -------------------------------------------------------------------------

    /**
     * Fills the crafting grid with ingredients for {@code recipe}, pulling items from the network.
     *
     * <p>Any existing grid contents that do not match the new recipe are returned to the network
     * first. For shaped recipes the ingredients are placed in the recipe's natural top-left
     * position; shapeless recipes fill slots 0–8 in order.
     */
    public void fillCraftingGridFromNetwork(ServerLevel level, CraftingRecipe recipe) {
        if (niPos == null) return;
        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        IItemHandler handler = ni.getItemHandler();
        if (handler == null) return;

        // Return current grid contents to the network.
        for (int i = 0; i < 9; i++) {
            ItemStack current = craftSlots.getItem(i);
            if (!current.isEmpty()) {
                handler.insertItem(0, current, false);
                craftSlots.setItem(i, ItemStack.EMPTY);
            }
        }

        // Place ingredients from the network into the grid.
        var ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            int h = shaped.getHeight();
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int recipeIdx = row * w + col;
                    int gridIdx = row * 3 + col;
                    if (recipeIdx < ingredients.size()) {
                        craftSlots.setItem(gridIdx, extractOneFromNetwork(handler, ingredients.get(recipeIdx)));
                    }
                }
            }
        } else {
            for (int i = 0; i < Math.min(ingredients.size(), 9); i++) {
                craftSlots.setItem(i, extractOneFromNetwork(handler, ingredients.get(i)));
            }
        }

        slotsChanged(craftSlots);
    }

    private static ItemStack extractOneFromNetwork(IItemHandler handler, Ingredient ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        for (int s = 0; s < handler.getSlots(); s++) {
            ItemStack inSlot = handler.getStackInSlot(s);
            if (!inSlot.isEmpty() && ingredient.test(inSlot)) {
                return handler.extractItem(s, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // Push-based real-time network inventory updates (Phase 7+8)
    // -------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!hasNetwork() || player.level().isClientSide()) return;
        if (!(player.level().getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        if (!(player instanceof ServerPlayer sp)) return;

        long rev = ni.getCacheRevision();
        if (rev != lastSentRevision) {
            KeyCounter inventory = ni.getCachedInventory();
            if (inventory != null) {
                PacketDistributor.sendToPlayer(sp, RequestSatContentsPacket.buildContentsPacket(inventory));
                lastSentRevision = rev;
            }
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider inner record
    // -------------------------------------------------------------------------

    public record Provider(BlockPos satPos, @Nullable BlockPos niPos) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.tremendousstorage.access_terminal");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new AccessTerminalMenu(id, inv, satPos, niPos);
        }
    }
}
