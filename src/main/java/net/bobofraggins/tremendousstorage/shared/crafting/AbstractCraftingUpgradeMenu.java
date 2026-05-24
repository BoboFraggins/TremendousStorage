package net.bobofraggins.tremendousstorage.shared.crafting;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
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

/**
 * Base class for all menus that may carry a crafting upgrade.
 *
 * <p>Holds the shared crafting-grid slots, the {@code refilling} flag that suppresses
 * intermediate {@link #slotsChanged} calls during a craft cycle, and the single
 * {@link RefillResultSlot} implementation used by every subclass. Subclasses supply only
 * their primary item source via {@link #createPrimarySource} and, when needed, override
 * {@link #doUpdateCraftResult} for extra result-slot logic (e.g. recipe disambiguation).
 *
 * <p>Slot layout when a crafting upgrade is present:
 * <ul>
 *   <li>[0]      craft result
 *   <li>[1..9]   3×3 crafting grid
 *   <li>[10+]    player inventory / hotbar (added by subclass)
 * </ul>
 */
public abstract class AbstractCraftingUpgradeMenu extends AbstractContainerMenu {

    protected final boolean hasCraftingUpgrade;

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    // Non-null only when hasCraftingUpgrade is true
    protected final CraftingContainer craftSlots;
    protected final ResultContainer resultSlots;

    protected final ContainerLevelAccess access;
    protected final Player player;

    // Slot index ranges (set by the subclass via constructor)
    protected final int invStart;
    protected final int invEnd;
    protected final int hotbarStart;
    protected final int hotbarEnd;

    // Suppresses slotsChanged during the craft+refill cycle so exactly one sync fires.
    private boolean refilling = false;

    // Tracks items produced during the current server-side doClick loop so we can stop
    // after one stack — mirrors the client's inventory-count prediction.
    private int serverShiftClickCrafted = 0;

    protected AbstractCraftingUpgradeMenu(
            MenuType<?> type,
            int id,
            Player player,
            boolean hasCraftingUpgrade,
            ContainerLevelAccess access,
            int invStart,
            int invEnd,
            int hotbarStart,
            int hotbarEnd) {
        super(type, id);
        this.hasCraftingUpgrade = hasCraftingUpgrade;
        this.player = player;
        this.access = access;
        this.invStart = invStart;
        this.invEnd = invEnd;
        this.hotbarStart = hotbarStart;
        this.hotbarEnd = hotbarEnd;
        if (hasCraftingUpgrade) {
            this.craftSlots = new TransientCraftingContainer(this, 3, 3);
            this.resultSlots = new ResultContainer();
        } else {
            this.craftSlots = null;
            this.resultSlots = null;
        }
    }

    // -------------------------------------------------------------------------
    // Subclass construction helpers
    // -------------------------------------------------------------------------

    /**
     * Adds slot 0 (result) and slots 1–9 (3×3 grid) to the menu.
     * Call this from the subclass constructor before adding player inventory slots.
     * Does nothing when no crafting upgrade is present.
     */
    protected void addCraftingSlots(int resultX, int resultY, int gridX, int gridY, int slotSize) {
        if (!hasCraftingUpgrade) return;
        addSlot(new RefillResultSlot(player, craftSlots, resultSlots, 0, resultX, resultY));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, gridX + col * slotSize, gridY + row * slotSize));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Abstract / overrideable
    // -------------------------------------------------------------------------

    /**
     * Returns the primary {@link CraftingRefillSource} for this menu.
     * Called once per craft take, server-side only.
     * The inventory is always tried as a second-pass fallback by the base class.
     */
    protected abstract CraftingRefillSource createPrimarySource(Player player);

    /**
     * Returns the recipe to use when consuming ingredients after a craft.
     * The default does a plain first-match lookup; AccessTerminalMenu overrides
     * this to return whichever recipe the player selected via disambiguation.
     */
    protected Optional<RecipeHolder<CraftingRecipe>> getActiveRecipe(CraftingInput input, Level level) {
        return ((ServerLevel) level).getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    /**
     * Recomputes the craft result for the current grid and pushes slot 0 to the client.
     * The default implementation does a plain first-match recipe lookup.
     * Override in menus that need disambiguation (e.g. AccessTerminalMenu).
     */
    protected void doUpdateCraftResult(Level level) {
        if (level.isClientSide()) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer sp = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional =
                ((ServerLevel) level).getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            ItemStack assembled = holder.value().assemble(input);
            if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            resultSlots.setRecipeUsed(holder);
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), 0, result));
    }

    // -------------------------------------------------------------------------
    // Shared overrides
    // -------------------------------------------------------------------------

    @Override
    public void slotsChanged(net.minecraft.world.Container inventory) {
        if (!hasCraftingUpgrade || refilling) return;
        access.execute((level, p) -> doUpdateCraftResult(level));
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput clickType, Player player) {
        if (!player.level().isClientSide()
                && hasCraftingUpgrade
                && slotId == 0
                && clickType == net.minecraft.world.inventory.ContainerInput.QUICK_MOVE) {
            serverShiftClickCrafted = 0;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (hasCraftingUpgrade && slot.container == resultSlots) return false;
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (hasCraftingUpgrade) {
            access.execute((level, p) -> clearContainer(player, craftSlots));
        }
        onCraftingMenuRemoved(player);
    }

    /**
     * Called from {@link #removed} after the crafting grid is cleared.
     * Subclasses override to add their own close-time behaviour (e.g. stopping chest lid
     * animation, syncing ender-backpack contents to remote storage).
     */
    protected void onCraftingMenuRemoved(Player player) {}

    // -------------------------------------------------------------------------
    // Clear-grid action
    // -------------------------------------------------------------------------

    /**
     * Clears all nine crafting grid slots. When {@code toStorage} is {@code true} each
     * item is first offered to {@link #returnToStorage}; any leftover (and all items when
     * {@code toStorage} is {@code false}) are added to the player's inventory, dropping on
     * the ground if the inventory is full.
     *
     * <p>Called server-side by
     * {@link net.bobofraggins.tremendousstorage.shared.network.ClearCraftingGridPacket}.
     */
    public void clearCraftingGrid(Player player, boolean toStorage) {
        if (!hasCraftingUpgrade) return;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) continue;
            craftSlots.setItem(i, ItemStack.EMPTY);
            ItemStack remainder = toStorage ? returnToStorage(player, stack) : stack;
            if (!remainder.isEmpty() && !player.addItem(remainder)) {
                player.drop(remainder, false);
            }
        }
        slotsChanged(craftSlots);
    }

    /**
     * Attempts to return {@code stack} to the backing storage for this menu.
     *
     * <p>The default implementation cannot return items to storage and returns
     * {@code stack} unchanged so the caller falls back to player inventory.
     * Subclasses (chest, backpack, access terminal) override this.
     *
     * @return the leftover items that could not be stored (may be {@code stack}
     *         unchanged, a smaller stack, or {@link ItemStack#EMPTY})
     */
    protected ItemStack returnToStorage(Player player, ItemStack stack) {
        return stack;
    }

    // -------------------------------------------------------------------------
    // Shared result-slot shift-click logic
    // -------------------------------------------------------------------------

    /**
     * Handles shift-clicking the craft result (slot 0).
     * Call from {@link #quickMoveStack} when {@code hasCraftingUpgrade && index == 0}.
     */
    protected final ItemStack quickMoveResult(Player player, Slot slot) {
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // Snapshot before onTake consumes ingredients — used for client-side prediction.
        ItemStack[] gridSnapshot = new ItemStack[9];
        for (int i = 0; i < 9; i++) gridSnapshot[i] = craftSlots.getItem(i).copy();
        if (!moveItemStackTo(stack, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
        slot.onQuickCraft(stack, original);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        // Client-side prediction: keep the shift-click loop going only while the player
        // has accumulated less than one full stack of the crafted item. Once they hit the
        // stack limit the prediction stops, ending the burst at one stack per shift-click.
        if (player.level().isClientSide()) {
            for (ItemStack s : gridSnapshot) {
                if (!s.isEmpty()) {
                    int total = 0;
                    for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                        ItemStack inv = player.getInventory().getItem(j);
                        if (!inv.isEmpty() && ItemStack.isSameItemSameComponents(inv, original)) {
                            total += inv.getCount();
                        }
                    }
                    if (total < original.getMaxStackSize()) {
                        resultSlots.setItem(0, original.copy());
                    }
                    break;
                }
            }
        }
        // Client: return non-empty so the local doClick loop continues running (drives the
        // one-stack prediction burst). Server: count items crafted this doClick call and
        // return EMPTY once one stack has been produced, stopping the server's while-loop at
        // the same point the client's prediction loop stops.
        if (player.level().isClientSide()) {
            return original;
        }
        serverShiftClickCrafted += original.getCount();
        return serverShiftClickCrafted < original.getMaxStackSize() ? original : ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // Shared RefillResultSlot
    // -------------------------------------------------------------------------

    protected class RefillResultSlot extends ResultSlot {
        protected RefillResultSlot(Player p, CraftingContainer craft, ResultContainer result, int idx, int x, int y) {
            super(p, craft, result, idx, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            // Fire crafting sounds, achievements, and recipe-book tracking on both sides
            // so the client hears the craft sound and the recipe book stays in sync.
            checkTakeAchievements(stack);

            if (player.level().isClientSide()) return;
            ServerLevel serverLevel = (ServerLevel) player.level();

            // Snapshot the grid before consumption so the refiller knows what types to
            // replace even after they are decremented below.
            ItemStack[] snapshot = new ItemStack[9];
            for (int i = 0; i < 9; i++) snapshot[i] = craftSlots.getItem(i).copy();

            // Reimplementation of ResultSlot.onTake's grid work, server-side only.
            // asPositionedCraftInput maps the compact recipe area to the full 3×3 grid
            // so that recipes smaller than 3×3 are handled correctly.
            CraftingInput.Positioned positioned = craftSlots.asPositionedCraftInput();
            CraftingInput input = positioned.input();
            int gridLeft = positioned.left();
            int gridTop = positioned.top();
            NonNullList<ItemStack> remaining = getActiveRecipe(input, serverLevel)
                    .map(h -> h.value().getRemainingItems(input))
                    .orElseGet(() -> NonNullList.withSize(input.size(), ItemStack.EMPTY));

            refilling = true;
            for (int y = 0; y < input.height(); y++) {
                for (int x = 0; x < input.width(); x++) {
                    int gridIndex = x + gridLeft + (y + gridTop) * craftSlots.getWidth();
                    ItemStack ingredient = craftSlots.getItem(gridIndex);
                    ItemStack rem = remaining.get(x + y * input.width());
                    if (!ingredient.isEmpty()) {
                        craftSlots.removeItem(gridIndex, 1);
                        ingredient = craftSlots.getItem(gridIndex);
                    }
                    if (!rem.isEmpty()) {
                        if (ingredient.isEmpty()) {
                            craftSlots.setItem(gridIndex, rem);
                        } else if (ItemStack.isSameItemSameComponents(ingredient, rem)) {
                            rem.grow(ingredient.getCount());
                            craftSlots.setItem(gridIndex, rem);
                        } else if (!player.addItem(rem)) {
                            player.drop(rem, false);
                        }
                    }
                }
            }

            // Refill empty slots from primary source (chest / backpack / network),
            // then fall back to the player's inventory.
            CraftingRefillSource primary = createPrimarySource(player);
            CraftingGridRefiller.refillFrom(primary, craftSlots, snapshot);
            primary.commit(player);
            CraftingGridRefiller.refillFrom(new InventoryRefillSource(player), craftSlots, snapshot);
            refilling = false;

            // One coherent sync: result slot then grid slots.
            slotsChanged(craftSlots);
            if (player instanceof ServerPlayer sp) {
                for (int i = 0; i < 9; i++) {
                    ItemStack current = craftSlots.getItem(i);
                    setRemoteSlot(i + 1, current);
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                            containerId, incrementStateId(), i + 1, current));
                }
            }
        }
    }
}
