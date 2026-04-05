package net.bobofraggins.intellistore.storage.bulkstorage;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bulk Storage Container block — stores up to {@value BulkStorageContainerBlockEntity#CAPACITY}
 * items in a shared pool across any number of distinct item types.
 *
 * <p>Accepts only items that Manila Folders accept: non-damageable items with default component
 * data (plain stackable items). There is no locking — any qualifying item may be freely
 * added or removed at any time.
 *
 * <p>There is no player-facing UI. All item movement is via hoppers, pipes, or any mod that
 * reads the {@link net.neoforged.neoforge.items.IItemHandler} capability.
 */
public class BulkStorageContainerBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<BulkStorageContainerBlock> CODEC = simpleCodec(BulkStorageContainerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final BooleanProperty[] H_PROPS = {NORTH, SOUTH, EAST, WEST};

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    @Override
    public MapCodec<BulkStorageContainerBlock> codec() {
        return CODEC;
    }

    public BulkStorageContainerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        return computeConnections(defaultBlockState().setValue(FACING, facing), ctx.getLevel(), ctx.getClickedPos());
    }

    private BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        for (int i = 0; i < HORIZONTALS.length; i++) {
            state = state.setValue(H_PROPS[i], canConnect(level, pos, HORIZONTALS[i]));
        }
        return state;
    }

    private static boolean canConnect(Level level, BlockPos pos, Direction dir) {
        Block neighbor = level.getBlockState(pos.relative(dir)).getBlock();
        return neighbor instanceof TubeBlock || neighbor instanceof NetworkConnector;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BulkStorageContainerBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                Registration.BULK_STORAGE_CONTAINER_BE_TYPE.get(),
                level.isClientSide()
                        ? BulkStorageContainerBlockEntity::clientTick
                        : BulkStorageContainerBlockEntity::serverTick);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide()) {
            BlockState updated = computeConnections(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, 2);
            }
            if (level.getBlockEntity(pos) instanceof BulkStorageContainerBlockEntity be) {
                be.setChanged();
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BulkStorageContainerBlockEntity be) {
            be.startOpen(player);
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof BulkStorageContainerBlockEntity bulk) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    bulk.saveToItem(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BulkStorageContainerBlockEntity be) {
                be.recheckOpeners(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
