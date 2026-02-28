package net.bobofraggins.intellistore.storage.accessterminal;

import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;

/**
 * The Storage Access Terminal block.
 *
 * <p>Right-clicking opens a UI that shows all items in the connected network
 * (via the nearest Network Interface), a 3×3 crafting grid, and the player's
 * inventory. Items can be extracted from or inserted into the network.
 *
 * <p>This block has no Block Entity — the Network Interface lookup is performed
 * at menu-open time and the NI position is passed through to the menu.
 */
public class AccessTerminalBlock extends Block implements NetworkConnector {

    public AccessTerminalBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos niPos = AccessTerminalBFS.findNI((ServerLevel) level, pos);

        // Power check: if NI is found but network is not powered, show message and do not open
        if (niPos != null && level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni
                && !ni.isPowered()) {
            player.displayClientMessage(
                    Component.translatable("screen.intellistore.not_enough_power"), true);
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new AccessTerminalMenu.Provider(pos, niPos), buf -> {
            buf.writeBlockPos(pos);
            buf.writeBoolean(niPos != null);
            if (niPos != null) buf.writeBlockPos(niPos);
        });
        return InteractionResult.SUCCESS;
    }

    /** Returns true if this block is still a SAT (used by menu's stillValid check). */
    public static boolean isStillValid(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == Registration.STORAGE_ACCESS_TERMINAL.get();
    }
}
