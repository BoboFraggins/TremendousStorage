package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Filing Cabinet priority/control screen.
 *
 * <p>No item slots. Carries two server→client data values via {@link ContainerData}:
 * <ul>
 *   <li>slot 0: priority ordinal (0–4)
 *   <li>slot 1: isOpen (0 = closed, 1 = open)
 * </ul>
 */
public class FilingCabinetMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor, called from the block entity's {@code createMenu}. */
    public FilingCabinetMenu(int windowId, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.FILING_CABINET_MENU.get(), windowId);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor, called via the {@link net.minecraft.world.inventory.MenuType} factory. */
    public FilingCabinetMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), new SimpleContainerData(2));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
    }

    public boolean isOpen() {
        return data.get(1) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
