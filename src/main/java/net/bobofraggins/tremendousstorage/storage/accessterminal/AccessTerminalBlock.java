package net.bobofraggins.tremendousstorage.storage.accessterminal;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * <p>The {@code active} blockstate is kept continuously in sync with the connected
 * NI's network validity state via a server tick in {@link AccessTerminalBlockEntity} (every 20 t).
 * It switches the screen texture to the glowing "on" variant.
 */
public class AccessTerminalBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<AccessTerminalBlock> CODEC = simpleCodec(AccessTerminalBlock::new);

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    // Body bounds (foot + body elements, ignoring keyboard/mouse): x=4-14, y=0-14, z=5-16
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_NORTH = Block.box(4, 0, 5, 14, 14, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_SOUTH = Block.box(2, 0, 0, 12, 14, 11);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_EAST = Block.box(0, 0, 4, 11, 14, 14);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_WEST = Block.box(5, 0, 2, 16, 14, 12);

    @Override
    public MapCodec<AccessTerminalBlock> codec() {
        return CODEC;
    }

    public AccessTerminalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition
                .any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
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
    public net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AccessTerminalBlockEntity be) {
            be.setChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccessTerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> ((AccessTerminalBlockEntity) be).serverTick();
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos niPos = AccessTerminalBFS.findNI((ServerLevel) level, pos);
        boolean craftingUpgrade = level.getBlockEntity(pos) instanceof AccessTerminalBlockEntity be
                && be.hasCraftingUpgrade();

        player.openMenu(new AccessTerminalMenu.Provider(pos, niPos, craftingUpgrade), buf -> {
            buf.writeBlockPos(pos);
            buf.writeBoolean(niPos != null);
            if (niPos != null) buf.writeBlockPos(niPos);
            buf.writeBoolean(craftingUpgrade);
        });
        return InteractionResult.SUCCESS;
    }

    /** Returns true if this block is still a SAT (used by menu's stillValid check). */
    public static boolean isStillValid(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == Registration.STORAGE_ACCESS_TERMINAL.get();
    }
}
