package net.bobofraggins.intellistore.storage.junkdrawer;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
 * The Junk Drawer block — stores up to {@value JunkDrawerBlockEntity#CAPACITY} individual items,
 * one per slot.
 *
 * <p>Accepts only items that Manila Folders reject: damageable items (tools, armour, weapons)
 * and items with non-default component data (enchanted books, named items, potions, etc.).
 * This is the precise complement of Bulk Storage / Manila Folders.
 *
 * <p>There is no locking — any qualifying item may be freely added or removed at any time.
 * All item movement is via hoppers, pipes, or any mod that reads the
 * {@link net.neoforged.neoforge.items.IItemHandler} capability.
 */
public class JunkDrawerBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<JunkDrawerBlock> CODEC = simpleCodec(JunkDrawerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final BooleanProperty[] H_PROPS = {NORTH, SOUTH, EAST, WEST};

    @Override
    public MapCodec<JunkDrawerBlock> codec() {
        return CODEC;
    }

    public JunkDrawerBlock(Properties props) {
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
        return new JunkDrawerBlockEntity(pos, state);
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
                Registration.JUNK_DRAWER_BE_TYPE.get(),
                level.isClientSide() ? JunkDrawerBlockEntity::clientTick : JunkDrawerBlockEntity::serverTick);
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
            if (level.getBlockEntity(pos) instanceof JunkDrawerBlockEntity be) {
                be.setChanged();
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof JunkDrawerBlockEntity be) {
            be.startOpen(player);
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof JunkDrawerBlockEntity junk) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem) {
                    junk.saveToItem(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof JunkDrawerBlockEntity be) {
                be.recheckOpeners(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
