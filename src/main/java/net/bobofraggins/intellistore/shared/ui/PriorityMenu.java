package net.bobofraggins.intellistore.shared.ui;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the priority screen shared by Junk Drawer and Bulk Storage Container.
 *
 * <p>No item slots. Carries one server→client data value:
 * <ul>
 *   <li>slot 0: priority ordinal (0–4)
 * </ul>
 */
public class PriorityMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public PriorityMenu(int windowId, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.PRIORITY_MENU.get(), windowId);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor. */
    public PriorityMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
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
