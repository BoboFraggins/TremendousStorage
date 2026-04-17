package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu for the Wireless Hub block.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0] Input slot — accepts unlinked Wireless SAT
 *   <li>[1] Output slot — linked Wireless SAT ready for retrieval
 *   <li>[2..28] Player main inventory (27 slots)
 *   <li>[29..37] Player hotbar (9 slots)
 * </ul>
 *
 * <p>One {@link ContainerData} value syncs the HAARP weather mode (ordinal) from server to client.
 *
 * <p>When {@code haarpUpgrade} is true the player inventory is shifted down by
 * {@link #HAARP_SECTION_H} pixels in the screen to make room for the weather-mode panel.
 */
public class WirelessHubMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int INV_START = 2;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    /** Height of the HAARP weather-control pane. Matches {@link HaarpWeatherPane#preferredHeight()}. */
    public static final int HAARP_SECTION_H = 68;

    /** Y position of player inventory when no HAARP upgrade. */
    public static final int INV_Y_BASE = 84;
    /** Y position of hotbar when no HAARP upgrade. */
    public static final int HOTBAR_Y_BASE = 142;

    private final BlockPos hubPos;
    private final boolean haarpUpgrade;
    private final ContainerData data;

    /** Server-side constructor. */
    public WirelessHubMenu(
            int id,
            Inventory inv,
            BlockPos hubPos,
            IItemHandler hubInventory,
            boolean haarpUpgrade,
            ContainerData data) {
        super(Registration.WIRELESS_HUB_MENU.get(), id);
        this.hubPos = hubPos;
        this.haarpUpgrade = haarpUpgrade;
        this.data = data;
        addDataSlots(data);

        int invY = haarpUpgrade ? INV_Y_BASE + HAARP_SECTION_H : INV_Y_BASE;
        int hotbarY = haarpUpgrade ? HOTBAR_Y_BASE + HAARP_SECTION_H : HOTBAR_Y_BASE;

        // Slot 0: input — Wireless SAT only
        addSlot(new SlotItemHandler(hubInventory, 0, 80, 20));

        // Slot 1: output — read-only for player (item placed by block entity)
        addSlot(new SlotItemHandler(hubInventory, 1, 80, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // player cannot put items here
            }
        });

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 7 + col * 18, invY + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 7 + col * 18, hotbarY));
        }
    }

    /** Client-side constructor. */
    public WirelessHubMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(2), buf.readBoolean(), new SimpleContainerData(1));
        // Note: initial mode value comes from ContainerData sync, not the buf integer
        buf.readInt(); // consume the int written by the server (mode ordinal)
    }

    public BlockPos getHubPos() {
        return hubPos;
    }

    public boolean hasHaarpUpgrade() {
        return haarpUpgrade;
    }

    /** Returns the current HAARP mode ordinal, synced from the server via ContainerData. */
    public int getHaarpModeOrdinal() {
        return data.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(hubPos.getX() + 0.5, hubPos.getY() + 0.5, hubPos.getZ() + 0.5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index == INPUT_SLOT || index == OUTPUT_SLOT) {
            // Move hub items to player inventory
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (index >= INV_START && index < HOTBAR_END) {
            // Move Wireless SAT from player inventory to hub input slot
            if (stack.getItem() instanceof PersonalAccessTerminalItem) {
                if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
