package net.bobofraggins.tremendousstorage.storage.accessterminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.crafting.AbstractCraftingUpgradeMenu;
import net.bobofraggins.tremendousstorage.shared.crafting.CraftingRefillSource;
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
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
public class AccessTerminalMenu extends AbstractCraftingUpgradeMenu {

    private final BlockPos satPos;

    @Nullable
    private final BlockPos niPos;

    // Recipe disambiguation — server-side only
    private List<RecipeHolder<CraftingRecipe>> matchingRecipes = new ArrayList<>();
    private int selectedRecipeIndex = 0;

    // Synced to client: number of recipes matching the current grid
    private final ContainerData recipeData = new SimpleContainerData(1);

    @Nullable
    private Identifier pendingPinRecipeId = null;

    /** Last cache revision we sent to this player; -1 = never sent. */
    private long lastSentRevision = -1;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Server-side constructor. Uses the default row count. */
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

    private AccessTerminalMenu(
            int id, Inventory inv, BlockPos satPos, @Nullable BlockPos niPos, boolean hasCraftingUpgrade, int rows) {
        super(
                Registration.STORAGE_ACCESS_TERMINAL_MENU.get(),
                id,
                inv.player,
                hasCraftingUpgrade,
                ContainerLevelAccess.create(inv.player.level(), satPos),
                hasCraftingUpgrade ? 10 : 0,
                hasCraftingUpgrade ? 37 : 27,
                hasCraftingUpgrade ? 37 : 27,
                hasCraftingUpgrade ? 46 : 36);
        this.satPos = satPos;
        this.niPos = niPos;

        final int S = AccessTerminalLayout.SLOT_SIZE;
        int networkPaneBottom = AccessTerminalLayout.TITLE_H + rows * S + 5;
        int playerInvY;

        if (hasCraftingUpgrade) {
            int craftingY = networkPaneBottom;
            playerInvY = craftingY + (AccessTerminalLayout.PLAYER_INV_Y - AccessTerminalLayout.CRAFTING_Y);
            addCraftingSlots(
                    AccessTerminalLayout.CRAFTING_RESULT_X,
                    craftingY + S,
                    AccessTerminalLayout.CRAFTING_GRID_X,
                    craftingY,
                    S);
            addDataSlots(recipeData);
        } else {
            playerInvY = networkPaneBottom + 20;
        }

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

    public int getMatchingRecipeCount() {
        return recipeData.get(0);
    }

    // -------------------------------------------------------------------------
    // Crafting — primary source and result override
    // -------------------------------------------------------------------------

    @Override
    protected CraftingRefillSource createPrimarySource(Player player) {
        return new NetworkRefillSource(player);
    }

    /** Uses the player-selected recipe rather than the first match, so disambiguation survives ingredient consumption. */
    @Override
    protected Optional<RecipeHolder<CraftingRecipe>> getActiveRecipe(CraftingInput input, Level level) {
        if (!matchingRecipes.isEmpty()) {
            return Optional.of(matchingRecipes.get(Math.min(selectedRecipeIndex, matchingRecipes.size() - 1)));
        }
        return Optional.empty();
    }

    /** Overrides the default recipe lookup to add multi-recipe disambiguation. */
    @Override
    protected void doUpdateCraftResult(Level level) {
        updateCraftingResult(level);
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

        List<RecipeHolder<CraftingRecipe>> newMatches = ((ServerLevel) level)
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

    private void updateResultSlot(Level level) {
        if (level.isClientSide()) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        if (!matchingRecipes.isEmpty()) {
            RecipeHolder<CraftingRecipe> holder = matchingRecipes.get(selectedRecipeIndex);
            ItemStack assembled = holder.value().assemble(input);
            if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            resultSlots.setRecipeUsed(holder);
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
     * Cycles the selected recipe by {@code direction} (+1 = next, -1 = previous).
     * Called by {@link net.bobofraggins.tremendousstorage.shared.network.CycleRecipePacket}.
     */
    public void handleCycleRecipe(int direction) {
        if (matchingRecipes.isEmpty()) return;
        selectedRecipeIndex = (selectedRecipeIndex + direction + matchingRecipes.size()) % matchingRecipes.size();
        access.execute((level, pos) -> updateResultSlot(level));
    }

    /**
     * Pins a specific recipe to be selected on the next {@link #doUpdateCraftResult} call.
     * Used by JEI/EMI/REI transfer handlers so the chosen recipe survives the
     * {@link #slotsChanged} that fires after the grid is filled.
     */
    public void setPendingPinRecipeId(Identifier id) {
        pendingPinRecipeId = id;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public boolean stillValid(Player player) {
        return AccessTerminalBlock.isStillValid(player.level(), satPos);
    }

    /**
     * Returns the {@link Level} in which the Network Interface lives.
     * Subclasses may override to redirect lookups to a different dimension.
     */
    protected Level getNiLevel(Player player) {
        return player.level();
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
        ItemStack copy = stack.copy();

        if (index >= invStart && index < hotbarEnd) {
            // Shift-click player slot: try network first, then swap between inv/hotbar.
            if (hasNetwork() && player.level().isClientSide()) return ItemStack.EMPTY;
            if (hasNetwork() && !player.level().isClientSide()) {
                if (getNiLevel(player).getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
                    ResourceHandler<ItemResource> handler = ni.getItemHandler();
                    if (handler != null) {
                        ItemResource resource = ItemResource.of(stack);
                        int inserted;
                        try (Transaction tx = Transaction.openRoot()) {
                            inserted = handler.insert(0, resource, stack.getCount(), tx);
                            tx.commit();
                        }
                        if (inserted == 0) return ItemStack.EMPTY;
                        ItemStack remainder = inserted >= stack.getCount()
                                ? ItemStack.EMPTY
                                : stack.copyWithCount(stack.getCount() - inserted);
                        slot.set(remainder);
                        if (hasCraftingUpgrade) slotsChanged(craftSlots);
                        ni.markContentsDirty();
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
        } else if (hasCraftingUpgrade && index >= 1 && index < 10) {
            if (!moveItemStackTo(stack, invStart, hotbarEnd, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    // -------------------------------------------------------------------------
    // Push-based real-time network inventory updates
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
    // Recipe viewer fill (JEI / EMI / REI)
    // -------------------------------------------------------------------------

    /**
     * Fills the crafting grid with ingredients for {@code recipe}, pulling items from the network
     * first and then falling back to the player's inventory for any slots the network could not fill.
     */
    public void fillCraftingGridFromNetwork(ServerLevel level, CraftingRecipe recipe) {
        if (!hasCraftingUpgrade || niPos == null) return;
        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        ResourceHandler<ItemResource> handler = ni.getItemHandler();
        if (handler == null) return;

        // Return current grid contents to the network
        for (int i = 0; i < 9; i++) {
            ItemStack current = craftSlots.getItem(i);
            if (current.isEmpty()) continue;
            int returned;
            try (Transaction tx = Transaction.openRoot()) {
                returned = handler.insert(0, ItemResource.of(current), current.getCount(), tx);
                tx.commit();
            }
            if (returned < current.getCount()) {
                ItemStack leftover = current.copyWithCount(current.getCount() - returned);
                if (!player.addItem(leftover)) player.drop(leftover, false);
            }
            craftSlots.setItem(i, ItemStack.EMPTY);
        }

        Ingredient[] slotIngredients = buildSlotIngredients(recipe);

        // First pass: extract from network
        for (int i = 0; i < 9; i++) {
            if (slotIngredients[i] != null && !slotIngredients[i].isEmpty()) {
                craftSlots.setItem(i, extractOneFromNetwork(handler, slotIngredients[i]));
            }
        }

        // Second pass: fill remaining empty slots from player inventory
        for (int i = 0; i < 9; i++) {
            if (!craftSlots.getItem(i).isEmpty()) continue;
            if (slotIngredients[i] == null || slotIngredients[i].isEmpty()) continue;
            Ingredient ing = slotIngredients[i];
            for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                ItemStack inv = player.getInventory().getItem(j);
                if (!inv.isEmpty() && ing.test(inv)) {
                    craftSlots.setItem(i, inv.split(1));
                    break;
                }
            }
        }

        slotsChanged(craftSlots);
    }

    private static Ingredient[] buildSlotIngredients(CraftingRecipe recipe) {
        Ingredient[] result = new Ingredient[9];
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
                            result[gridIdx] = compactIngredients.get(ingredIdx);
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < Math.min(slotMapping.size(), 9); i++) {
                int ingredIdx = slotMapping.getInt(i);
                if (ingredIdx >= 0 && ingredIdx < compactIngredients.size()) {
                    result[i] = compactIngredients.get(ingredIdx);
                }
            }
        }
        return result;
    }

    private static ItemStack extractOneFromNetwork(ResourceHandler<ItemResource> handler, Ingredient ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        for (int s = 0; s < handler.size(); s++) {
            ItemResource res = handler.getResource(s);
            if (res.isEmpty()) continue;
            if (!ingredient.test(res.toStack(1))) continue;
            int extracted;
            try (Transaction tx = Transaction.openRoot()) {
                extracted = handler.extract(s, res, 1, tx);
                if (extracted > 0) tx.commit();
            }
            if (extracted > 0) return res.toStack(extracted);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack tryExtractFromNetwork(ResourceHandler<ItemResource> handler, ItemStack template) {
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource res = handler.getResource(slot);
            if (res.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(res.toStack(1), template)) continue;
            int extracted;
            try (Transaction tx = Transaction.openRoot()) {
                extracted = handler.extract(slot, res, 1, tx);
                if (extracted > 0) tx.commit();
            }
            if (extracted > 0) return res.toStack(extracted);
        }
        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // Network refill source
    // -------------------------------------------------------------------------

    private class NetworkRefillSource implements CraftingRefillSource {
        @Nullable
        private final ResourceHandler<ItemResource> handler;

        NetworkRefillSource(Player player) {
            NetworkInterfaceBlockEntity ni = null;
            if (niPos != null && getNiLevel(player).getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity n) {
                ni = n;
            }
            this.handler = ni != null ? ni.getItemHandler() : null;
        }

        @Override
        public ItemStack extractOne(ItemStack template) {
            if (handler == null) return ItemStack.EMPTY;
            return tryExtractFromNetwork(handler, template.copyWithCount(1));
        }

        @Override
        public void returnOne(ItemStack stack) {
            if (handler == null) return;
            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(0, ItemResource.of(stack), 1, tx);
                tx.commit();
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
