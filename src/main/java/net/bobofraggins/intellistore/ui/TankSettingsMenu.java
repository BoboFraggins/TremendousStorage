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
 * Minimal menu for the three tank settings screens (Fluid Tank, Gas Tank, Source Tank).
 *
 * <p>No item slots. Carries one server→client data value via {@link ContainerData}:
 * <ul>
 *   <li>slot 0: voidExcess (0 = off, 1 = on)
 * </ul>
 */
public class TankSettingsMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor, called from the block's {@code useWithoutItem}. */
    public TankSettingsMenu(int windowId, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.TANK_SETTINGS_MENU.get(), windowId);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor, called via the {@link net.minecraft.world.inventory.MenuType} factory. */
    public TankSettingsMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isVoidExcess() {
        return data.get(0) == 1;
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
