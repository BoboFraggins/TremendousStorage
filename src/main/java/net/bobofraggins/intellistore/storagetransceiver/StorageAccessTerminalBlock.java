package net.bobofraggins.intellistore.storagetransceiver;

import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.StorageAccessTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
public class StorageAccessTerminalBlock extends Block {

    public StorageAccessTerminalBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos niPos = StorageAccessTerminalBFS.findNI((ServerLevel) level, pos);
        player.openMenu(
                new StorageAccessTerminalMenu.Provider(pos, niPos),
                buf -> {
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
