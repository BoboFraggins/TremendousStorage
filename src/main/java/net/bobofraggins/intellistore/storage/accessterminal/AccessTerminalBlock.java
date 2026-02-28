package net.bobofraggins.intellistore.storage.accessterminal;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 *
 * <p>The {@code active} blockstate is {@code true} when a powered NI is reachable,
 * switching the screen texture to the glowing "on" variant. It is updated each time
 * the block is interacted with.
 */
public class AccessTerminalBlock extends Block implements NetworkConnector {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AccessTerminalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition
                .any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(
                        BlockStateProperties.HORIZONTAL_FACING,
                        ctx.getHorizontalDirection().getOpposite())
                .setValue(ACTIVE, false);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos niPos = AccessTerminalBFS.findNI((ServerLevel) level, pos);

        boolean powered = niPos != null
                && level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni
                && ni.isPowered();

        // Update active state to reflect current network status
        boolean currentlyActive = state.getValue(ACTIVE);
        if (powered != currentlyActive) {
            level.setBlock(pos, state.setValue(ACTIVE, powered), 3);
        }

        // Power check: if NI is found but network is not powered, show message and do not open
        if (niPos != null && !powered) {
            player.displayClientMessage(Component.translatable("screen.intellistore.not_enough_power"), true);
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
