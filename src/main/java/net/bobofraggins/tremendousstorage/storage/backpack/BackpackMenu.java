package net.bobofraggins.tremendousstorage.storage.backpack;

import java.util.Optional;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
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
public class BackpackMenu extends AbstractContainerMenu {

    private final int slotType;
    private final int slotIndex;
    private final String slotId;
    private final ContainerData data;
    private final boolean hasCraftingUpgrade;

    // Crafting containers — non-null only when hasCraftingUpgrade is true
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final ContainerLevelAccess access;
    private final Player player;

    // Slot index ranges — adjusted per hasCraftingUpgrade
    private final int invStart;
    private final int hotbarEnd;

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
        super(menuType, syncId);
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.slotId = slotId;
        this.data = data;
        this.hasCraftingUpgrade = hasCraftingUpgrade;
        this.player = playerInv.player;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), playerInv.player.blockPosition());

        // When the crafting upgrade is present the CraftingGridPane replaces the blank(20) spacer,
        // so craftY is 20 px earlier than playerInvY in the no-crafting layout.
        int playerInvY;

        if (hasCraftingUpgrade) {
            this.craftSlots = new TransientCraftingContainer(this, 3, 3);
            this.resultSlots = new ResultContainer();

            int craftY = 29 + rows * 18; // title(17) + blank(7) + inventoryPane gap(5) = 29

            // Slot 0: craft result (row-1 position, x=120 — matches CraftingGridPane.RESULT_LOCAL_Y)
            addSlot(new ResultSlot(playerInv.player, craftSlots, resultSlots, 0, 120, craftY + 18));

            // Slots 1-9: 3×3 crafting grid
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, craftY + row * 18));
                }
            }

            // Player inventory starts after the crafting pane (3 × 18 + 4 px gap = 58)
            playerInvY = craftY + 3 * 18 + 4;
            this.invStart = 10;
            this.hotbarEnd = 46;
        } else {
            this.craftSlots = null;
            this.resultSlots = null;
            playerInvY = 49 + rows * 18; // title(17) + blank(7) + inventoryPane gap(5) + blank(20) = 49
            this.invStart = 0;
            this.hotbarEnd = 36;
        }

        int hotbarY = playerInvY + 58;

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 20 + col * 18, playerInvY + row * 18));
            }
        }
        // Player hotbar
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

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    /** Called from {@link net.bobofraggins.tremendousstorage.shared.network.SetBackpackPriorityPacket}. */
    public void setPriorityInData(int ordinal) {
        data.set(0, ordinal);
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
        if (level.isClientSide()) return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer sp = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = ((net.minecraft.server.level.ServerLevel) level)
                .getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            if (resultSlots.setRecipeUsed(sp, holder)) {
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
        onMenuRemoved(player);
    }

    /** Hook called after the menu is closed. Subclasses (e.g. Ender Backpack) override this to sync contents to storage. */
    protected void onMenuRemoved(Player player) {}

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (hasCraftingUpgrade && slot.container == resultSlots) return false;
        return super.canTakeItemForPickAll(stack, slot);
    }

    // -------------------------------------------------------------------------
    // Menu lifecycle
    // -------------------------------------------------------------------------

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
}
