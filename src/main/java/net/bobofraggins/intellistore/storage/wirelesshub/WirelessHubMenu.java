package net.bobofraggins.intellistore.storage.wirelesshub;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 */
public class WirelessHubMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int INV_START = 2;
    private static final int INV_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    private final BlockPos hubPos;
    private final IItemHandler hubInventory;

    /** Server-side constructor. */
    public WirelessHubMenu(int id, Inventory inv, BlockPos hubPos, IItemHandler hubInventory) {
        super(Registration.WIRELESS_HUB_MENU.get(), id);
        this.hubPos = hubPos;
        this.hubInventory = hubInventory;

        // Slot 0: input — Wireless SAT only
        addSlot(new SlotItemHandler(hubInventory, 0, 62, 35));

        // Slot 1: output — read-only for player (item placed by block entity)
        addSlot(new SlotItemHandler(hubInventory, 1, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // player cannot put items here
            }
        });

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    /** Client-side constructor. */
    public WirelessHubMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(2));
    }

    public BlockPos getHubPos() {
        return hubPos;
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
