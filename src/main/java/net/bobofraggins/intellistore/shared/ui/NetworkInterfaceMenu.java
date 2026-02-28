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
 * Menu for the Network Interface block.
 *
 * <p>No item slots. Carries one server→client data value:
 * <ul>
 *   <li>slot 0: 1 if network is valid, 0 if invalid
 * </ul>
 */
public class NetworkInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public NetworkInterfaceMenu(int windowId, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.NETWORK_INTERFACE_MENU.get(), windowId);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor — reads {@link BlockPos} from the network buffer. */
    public NetworkInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readBlockPos(), new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    /** Returns {@code true} if the connected network is valid (exactly one NI). */
    public boolean isNetworkValid() {
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
