package net.bobofraggins.tremendousstorage.shared.ui;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Menu for the tank settings screen.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>0 — fluid-container input (left pane, top)
 *   <li>1 — fluid-container output (left pane, bottom; output-only)
 *   <li>2–28 — player inventory
 *   <li>29–37 — hotbar
 * </ul>
 *
 * <p>One server→client data value via {@link ContainerData}:
 * <ul>
 *   <li>slot 0: voidExcess (0 = off, 1 = on)
 * </ul>
 */
public class TankSettingsMenu extends AbstractContainerMenu {

    // Fluid-transfer slot positions — centred horizontally in the 176 px panel: (176-16)/2 = 80
    public static final int FLUID_IN_X = 80;
    public static final int FLUID_IN_Y = 20;
    public static final int FLUID_OUT_X = 80;
    public static final int FLUID_OUT_Y = 62;

    // Player-inventory slot positions — centred in 176 px: (176-162)/2 = 7
    // Y values match PlayerInventoryPane starting at Dialog.TITLE_H(17) + settings pane(71) = 88.
    public static final int INV_START_X = 7;
    private static final int INV_Y = 88;
    private static final int HOTBAR_Y = 146;

    private final BlockPos pos;
    private final ContainerData data;
    private final boolean hasPullerUpgrade;

    /** Server-side constructor, called from the block's {@code useWithoutItem}. */
    public TankSettingsMenu(
            int windowId, Inventory inv, BlockPos pos, ContainerData data, SimpleContainer transferContainer) {
        this(windowId, inv, pos, data, transferContainer, false);
    }

    /** Server-side constructor with puller upgrade flag. */
    public TankSettingsMenu(
            int windowId,
            Inventory inv,
            BlockPos pos,
            ContainerData data,
            SimpleContainer transferContainer,
            boolean hasPullerUpgrade) {
        super(Registration.TANK_SETTINGS_MENU.get(), windowId);
        this.pos = pos;
        this.data = data;
        this.hasPullerUpgrade = hasPullerUpgrade;
        addDataSlots(data);

        // Slot 0: fluid input — accepts any item with IFluidHandlerItem
        addSlot(new Slot(transferContainer, 0, FLUID_IN_X, FLUID_IN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
            }
        });

        // Slot 1: output-only — player takes the result, cannot place
        addSlot(new Slot(transferContainer, 1, FLUID_OUT_X, FLUID_OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (slots 2–28)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_START_X + col * 18, INV_Y + row * 18));
            }
        }
        // Hotbar (slots 29–37)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_START_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side constructor — reads BlockPos and puller flag from network buffer. */
    public TankSettingsMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), new SimpleContainerData(1), new SimpleContainer(2), buf.readBoolean());
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isVoidExcess() {
        return data.get(0) == 1;
    }

    public boolean hasPullerUpgrade() {
        return hasPullerUpgrade;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 2) {
            // Fluid slots → player inventory + hotbar
            if (!moveItemStackTo(stack, 2, 38, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            // Player inventory → try fluid input first, then hotbar
            if (!moveItemStackTo(stack, 0, 1, false)) {
                if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
            }
        } else {
            // Hotbar → try fluid input first, then player inventory
            if (!moveItemStackTo(stack, 0, 1, false)) {
                if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else if (stack.getCount() != original.getCount()) slot.setChanged();
        else return ItemStack.EMPTY;

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
