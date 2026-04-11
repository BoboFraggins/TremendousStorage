package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Wireless Hub block.
 *
 * <p>Right-clicking opens a 2-slot UI. The left slot accepts an unlinked Wireless SAT;
 * when placed there the hub reads the Network Interface position from the BFS scan and
 * records it into the item as a data component, then moves the item to the right slot
 * for retrieval. Only works when the network is valid.
 */
public class WirelessHubBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<WirelessHubBlock> CODEC = simpleCodec(WirelessHubBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    public MapCodec<WirelessHubBlock> codec() {
        return CODEC;
    }

    public WirelessHubBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessHubBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // All geometry (base, rods, arc) is drawn by WirelessHubRenderer; JSON provides particle only.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WirelessHubBlockEntity be) {
            be.setChanged();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof MenuProvider mp) {
            player.openMenu(mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> ((WirelessHubBlockEntity) be).clientTick();
        } else {
            return (lvl, pos, st, be) -> ((WirelessHubBlockEntity) be).serverTick();
        }
    }
}
