package net.bobofraggins.intellistore.external.arsnouveau;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Source Tank block — stores up to {@link SourceTankBlockEntity#CAPACITY} source
 * (10× a vanilla Ars Nouveau Source Jar).
 *
 * <p>Source is moved into and out of this tank via the Ars Nouveau source network:
 * source relays, arcane pedestals, obelisks, and any other block that queries the
 * {@link com.hollingsworth.arsnouveau.api.source.ISourceCap} block capability.
 * There is no right-click item interaction because source does not exist as a handheld
 * item container in Ars Nouveau.
 */
public class SourceTankBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<SourceTankBlock> CODEC = simpleCodec(SourceTankBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SourceTankBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<SourceTankBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceTankBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Right-click with empty hand → open tank settings screen. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SourceTankBlockEntity be)) return InteractionResult.FAIL;
        player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
