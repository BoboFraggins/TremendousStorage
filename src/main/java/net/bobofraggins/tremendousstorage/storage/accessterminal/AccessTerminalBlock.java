package net.bobofraggins.tremendousstorage.storage.accessterminal;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
            @Nullable Orientation orientation,
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
        return level.isClientSide() ? null : (lvl, pos, st, be) -> ((AccessTerminalBlockEntity) be).serverTick();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof AccessTerminalBlockEntity terminal) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem) {
                    TagValueOutput _beOut = TagValueOutput.createWithContext(
                            ProblemReporter.DISCARDING, params.getLevel().registryAccess());
                    terminal.saveCustomOnly(_beOut);
                    BlockItem.setBlockEntityData(drop, terminal.getType(), _beOut);
                }
            }
        }
        return drops;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos niPos = AccessTerminalBFS.findNI((ServerLevel) level, pos);
        if (niPos == null) return InteractionResult.CONSUME;
        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni
                && ni.isNetworkValid()
                && ni.isPowered())) {
            return InteractionResult.CONSUME;
        }

        boolean craftingUpgrade =
                level.getBlockEntity(pos) instanceof AccessTerminalBlockEntity be && be.hasCraftingUpgrade();

        player.openMenu(new AccessTerminalMenu.Provider(pos, niPos, craftingUpgrade), buf -> {
            buf.writeBlockPos(pos);
            buf.writeBoolean(true);
            buf.writeBlockPos(niPos);
            buf.writeBoolean(craftingUpgrade);
        });
        return InteractionResult.SUCCESS;
    }

    /** Returns true if this block is still a SAT (used by menu's stillValid check). */
    public static boolean isStillValid(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == Registration.STORAGE_ACCESS_TERMINAL.get();
    }
}
