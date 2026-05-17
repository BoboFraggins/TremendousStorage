package net.bobofraggins.tremendousstorage.storage.accessterminal;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.network.RequestSatContentsPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

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

    private final boolean hasCraftingUpgrade;

    // Slot indices — depend on hasCraftingUpgrade
    private final int resultSlot; // -1 when no crafting
    private final int craftStart; // -1 when no crafting
    private final int craftEnd; // -1 when no crafting
    private final int invStart;
    private final int invEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final ContainerLevelAccess access;
    private final Player player;

    // Recipe disambiguation — server-side only
    private List<RecipeHolder<CraftingRecipe>> matchingRecipes = new ArrayList<>();
    private int selectedRecipeIndex = 0;

    // Synced to client: number of recipes matching the current grid
    private final ContainerData recipeData = new SimpleContainerData(1);

    @Nullable
    private Identifier pendingPinRecipeId = null;

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
    public AccessTerminalMenu(
            int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos, boolean hasCraftingUpgrade) {
        this(id, inv, satPos, niPos, hasCraftingUpgrade, TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
    }

    /** Client-side factory. Decodes the buf then computes rows from the current screen height. */
    public static AccessTerminalMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos satPos = buf.readBlockPos();
        BlockPos niPos = buf.readBoolean() ? buf.readBlockPos() : null;
        boolean hasCraftingUpgrade = buf.readBoolean();
        return new AccessTerminalMenu(
                id,
                inv,
                satPos,
                niPos,
                hasCraftingUpgrade,
                TremendousStorageClientConfig.computeVisibleRows(hasCraftingUpgrade));
    }

    /**
     * Internal constructor that computes all slot Y positions from the given network grid row
     * count.
     *
     * <p>With crafting: title(17) + networkPane(rows×18+5) + craftingPane(58) + playerInv
     * → craftingY = 22 + rows×18; fixed 58-px offsets to player inv and hotbar.
     * <p>Without crafting: crafting pane is omitted; player inv follows directly after network pane.
     */
    private AccessTerminalMenu(
            int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos, boolean hasCraftingUpgrade, int rows) {
        super(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), id);
        this.satPos = satPos;
        this.niPos = niPos;
        this.hasCraftingUpgrade = hasCraftingUpgrade;
        this.player = inv.player;
        this.access = ContainerLevelAccess.create(inv.player.level(), satPos);

        final int S = AccessTerminalLayout.SLOT_SIZE;
        int networkPaneBottom = AccessTerminalLayout.TITLE_H + rows * S + 5;
        int playerInvY;

        if (hasCraftingUpgrade) {
            this.craftSlots = new TransientCraftingContainer(this, 3, 3);
            this.resultSlots = new ResultContainer();

            int craftingY = networkPaneBottom;
            playerInvY = craftingY + (AccessTerminalLayout.PLAYER_INV_Y - AccessTerminalLayout.CRAFTING_Y);

            this.resultSlot = 0;
            this.craftStart = 1;
            this.craftEnd = 10;
            this.invStart = 10;
            this.invEnd = 37;
            this.hotbarStart = 37;
            this.hotbarEnd = 46;

            // Slot 0: craft result
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

            addDataSlots(recipeData);
        } else {
            this.craftSlots = null;
            this.resultSlots = null;
            playerInvY = networkPaneBottom + 20;

            this.resultSlot = -1;
            this.craftStart = -1;
            this.craftEnd = -1;
            this.invStart = 0;
            this.invEnd = 27;
            this.hotbarStart = 27;
            this.hotbarEnd = 36;
        }

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        inv, col + row * 9 + 9, AccessTerminalLayout.PLAYER_INV_X + col * S, playerInvY + row * S));
            }
        }

        int hotbarY = playerInvY + (AccessTerminalLayout.HOTBAR_Y - AccessTerminalLayout.PLAYER_INV_Y);
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

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    public int getMatchingRecipeCount() {
        return recipeData.get(0);
    }

    // -------------------------------------------------------------------------
    // Crafting
    // -------------------------------------------------------------------------

    @Override
    public void slotsChanged(net.minecraft.world.Container inventory) {
        if (!hasCraftingUpgrade) return;
        access.execute((level, pos) -> updateCraftingResult(level));
    }

    /**
     * Collects all crafting recipes that match the current grid, selects the right one
     * (preserving the cycle index when the recipe list is unchanged, or pinning to a
     * specific recipe when one was requested via {@link #setPendingPinRecipeId}), then
     * pushes the result stack to the client.
     */
    private void updateCraftingResult(Level level) {
        if (level.isClientSide()) return;
        CraftingInput input = craftSlots.asCraftInput();

        List<RecipeHolder<CraftingRecipe>> newMatches = ((net.minecraft.server.level.ServerLevel) level)
                .getServer().getRecipeManager().recipeMap().byType(RecipeType.CRAFTING).stream()
                        .filter(h -> h.value().matches(input, level))
                        .toList();

        if (!recipeListsEqual(matchingRecipes, newMatches)) {
            matchingRecipes = newMatches;
            if (pendingPinRecipeId != null) {
                selectedRecipeIndex = 0;
                for (int i = 0; i < newMatches.size(); i++) {
                    if (newMatches.get(i).id().equals(pendingPinRecipeId)) {
                        selectedRecipeIndex = i;
                        break;
                    }
                }
                pendingPinRecipeId = null;
            } else {
                selectedRecipeIndex = 0;
            }
        } else {
            matchingRecipes = newMatches;
            if (!matchingRecipes.isEmpty()) {
                selectedRecipeIndex = Math.max(0, Math.min(selectedRecipeIndex, matchingRecipes.size() - 1));
            }
        }

        recipeData.set(0, matchingRecipes.size());
        updateResultSlot(level);
    }

    /** Assembles the result from the currently selected recipe and sends it to the client. */
    private void updateResultSlot(Level level) {
        if (level.isClientSide()) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        if (!matchingRecipes.isEmpty()) {
            RecipeHolder<CraftingRecipe> holder = matchingRecipes.get(selectedRecipeIndex);
            if (resultSlots.setRecipeUsed(serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), 0, result));
    }

    private static boolean recipeListsEqual(
            List<RecipeHolder<CraftingRecipe>> a, List<RecipeHolder<CraftingRecipe>> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).id().equals(b.get(i).id())) return false;
        }
        return true;
    }

    /**
     * Cycles the selected recipe by {@code direction} (+1 = next, -1 = previous), wrapping
     * around. Called by {@link net.bobofraggins.tremendousstorage.shared.network.CycleRecipePacket}.
     */
    public void handleCycleRecipe(int direction) {
        if (matchingRecipes.isEmpty()) return;
        selectedRecipeIndex = (selectedRecipeIndex + direction + matchingRecipes.size()) % matchingRecipes.size();
        access.execute((level, pos) -> updateResultSlot(level));
    }

    /**
     * Pins a specific recipe to be selected on the next {@link #updateCraftingResult} call.
     * Used by JEI/EMI/REI transfer handlers so the chosen recipe survives the
     * {@link #slotsChanged} that fires after the grid is filled.
     */
    public void setPendingPinRecipeId(Identifier id) {
        pendingPinRecipeId = id;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (hasCraftingUpgrade) {
            access.execute((level, pos) -> clearContainer(player, craftSlots));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return AccessTerminalBlock.isStillValid(player.level(), satPos);
    }

    /**
     * Returns the {@link Level} in which the Network Interface lives.
     * Subclasses may override to redirect lookups to a different dimension
     * (e.g. the Wireless SAT when the player is cross-dimension).
     */
    protected Level getNiLevel(Player player) {
        return player.level();
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

        if (hasCraftingUpgrade && index == resultSlot) {
            // Snapshot the craft grid before ingredients are consumed by onTake
            ItemStack[] gridSnapshot = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                gridSnapshot[i] = craftSlots.getItem(i).copy();
            }

            // Shift-click result: move to inventory
            if (!moveItemStackTo(stack, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            player.drop(stack, false);

            // Refill depleted craft slots from the network
            refillCraftGridFromNetwork(player, gridSnapshot);

            return copy;
        } else if (index >= invStart && index < hotbarEnd) {
            // Shift-click player slot: try network first, then swap between inv/hotbar.
            // Skip client-side prediction when network is present — server inserts into network,
            // but client would predict a vanilla inv↔hotbar swap, causing a flicker.
            if (hasNetwork() && player.level().isClientSide()) return ItemStack.EMPTY;
            if (hasNetwork() && !player.level().isClientSide()) {
                if (getNiLevel(player).getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
                    ResourceHandler<ItemResource> handler = ni.getItemHandler();
                    if (handler != null) {
                        ItemResource resource = ItemResource.of(stack);
                        int inserted = handler.insert(0, resource, stack.getCount(), null);
                        ItemStack remainder = inserted >= stack.getCount()
                                ? ItemStack.EMPTY
                                : stack.copyWithCount(stack.getCount() - inserted);
                        slot.set(remainder);
                        if (hasCraftingUpgrade) slotsChanged(craftSlots);
                        // Refresh the client's network grid
                        KeyCounter inventory = ni.getCachedInventory();
                        if (inventory != null && player instanceof ServerPlayer sp) {
                            PacketDistributor.sendToPlayer(
                                    sp,
                                    RequestSatContentsPacket.buildContentsPacket(inventory, ni.getFluidStorageKeys()));
                        }
                        return copy;
                    }
                }
            }
            // Fallback: swap between main inv and hotbar
            if (index < invEnd) {
                if (!moveItemStackTo(stack, hotbarStart, hotbarEnd, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, invStart, invEnd, false)) return ItemStack.EMPTY;
            }
        } else if (hasCraftingUpgrade && index >= craftStart && index < craftEnd) {
            if (!moveItemStackTo(stack, invStart, hotbarEnd, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (index == resultSlot) player.drop(stack, false);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (hasCraftingUpgrade && slot.container == resultSlots) return false;
        return super.canTakeItemForPickAll(stack, slot);
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
        if (!hasCraftingUpgrade || !hasNetwork() || player.level().isClientSide()) return;
        if (!(getNiLevel(player).getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        ResourceHandler<ItemResource> handler = ni.getItemHandler();
        if (handler == null) return;

        boolean anyRefilled = false;
        for (int i = 0; i < 9; i++) {
            ItemStack snap = gridSnapshot[i];
            if (snap.isEmpty()) continue; // slot was empty before craft, nothing to refill

            ItemStack current = craftSlots.getItem(i);

            if (current.isEmpty()) {
                // Normal case: ingredient was fully consumed — pull a fresh stack from network
                anyRefilled |= extractFromNetworkIntoSlot(handler, snap, i);
            } else if (!snap.getItem().getCraftingRemainder(snap).create().isEmpty()) {
                // Container item case (e.g. lava bucket → empty bucket):
                // onTake placed the remainder (empty bucket) back in the slot.
                // If that remainder matches what's there now, swap it for a full one.
                ItemStack remainder = snap.getItem().getCraftingRemainder(snap).create();
                if (!remainder.isEmpty() && ItemStack.isSameItemSameComponents(current, remainder)) {
                    // Try to extract the full item (e.g. lava bucket) from the network
                    ItemStack needed = snap.copyWithCount(1);
                    ItemStack extracted = tryExtractFromNetwork(handler, needed);
                    if (!extracted.isEmpty()) {
                        // Return the remainder (empty bucket) to the network
                        handler.insert(0, ItemResource.of(current), 1, null);
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
    private boolean extractFromNetworkIntoSlot(
            ResourceHandler<ItemResource> handler, ItemStack template, int gridIndex) {
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
    private static ItemStack tryExtractFromNetwork(ResourceHandler<ItemResource> handler, ItemStack template) {
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource res = handler.getResource(slot);
            if (res.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(res.toStack(1), template)) continue;
            int extracted = handler.extract(slot, res, template.getMaxStackSize(), null);
            if (extracted > 0) return res.toStack(extracted);
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
        if (!hasCraftingUpgrade || niPos == null) return;
        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        ResourceHandler<ItemResource> handler = ni.getItemHandler();
        if (handler == null) return;

        // Return current grid contents to the network.
        for (int i = 0; i < 9; i++) {
            ItemStack current = craftSlots.getItem(i);
            if (!current.isEmpty()) {
                handler.insert(0, ItemResource.of(current), current.getCount(), null);
                craftSlots.setItem(i, ItemStack.EMPTY);
            }
        }

        // Place ingredients from the network into the grid.
        var placementInfo = recipe.placementInfo();
        var compactIngredients = placementInfo.ingredients();
        var slotMapping = placementInfo.slotsToIngredientIndex();
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            int h = shaped.getHeight();
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int recipeIdx = row * w + col;
                    int gridIdx = row * 3 + col;
                    if (recipeIdx < slotMapping.size()) {
                        int ingredIdx = slotMapping.getInt(recipeIdx);
                        if (ingredIdx >= 0 && ingredIdx < compactIngredients.size()) {
                            craftSlots.setItem(
                                    gridIdx, extractOneFromNetwork(handler, compactIngredients.get(ingredIdx)));
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < Math.min(slotMapping.size(), 9); i++) {
                int ingredIdx = slotMapping.getInt(i);
                if (ingredIdx >= 0 && ingredIdx < compactIngredients.size()) {
                    craftSlots.setItem(i, extractOneFromNetwork(handler, compactIngredients.get(ingredIdx)));
                }
            }
        }

        slotsChanged(craftSlots);
    }

    private static ItemStack extractOneFromNetwork(ResourceHandler<ItemResource> handler, Ingredient ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        for (int s = 0; s < handler.size(); s++) {
            ItemResource res = handler.getResource(s);
            if (res.isEmpty()) continue;
            ItemStack inSlot = res.toStack(1);
            if (ingredient.test(inSlot)) {
                int extracted = handler.extract(s, res, 1, null);
                return extracted > 0 ? res.toStack(extracted) : ItemStack.EMPTY;
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
        if (!(getNiLevel(player).getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        if (!(player instanceof ServerPlayer sp)) return;

        long rev = ni.getCacheRevision();
        if (rev != lastSentRevision) {
            KeyCounter inventory = ni.getCachedInventory();
            if (inventory != null) {
                PacketDistributor.sendToPlayer(
                        sp, RequestSatContentsPacket.buildContentsPacket(inventory, ni.getFluidStorageKeys()));
                lastSentRevision = rev;
            }
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider inner record
    // -------------------------------------------------------------------------

    public record Provider(BlockPos satPos, @Nullable BlockPos niPos, boolean hasCraftingUpgrade)
            implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.tremendousstorage.access_terminal");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new AccessTerminalMenu(id, inv, satPos, niPos, hasCraftingUpgrade);
        }
    }
}
