package net.bobofraggins.intellistore.networkinterface;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A two-block-tall Network Interface block.
 *
 * <p>Uses the Door/Bed pattern: one {@link Block} type registered once; the blockstate
 * property {@link BlockStateProperties#DOUBLE_BLOCK_HALF} differentiates the lower (real)
 * half from the upper (dummy) half. The {@link BlockEntity} only exists on the lower half.
 *
 * <p><b>Placement:</b> requires the block directly above to be replaceable; otherwise
 * placement is cancelled. Places the upper half automatically in {@link #setPlacedBy}.
 *
 * <p><b>Removal:</b> breaking either half removes the other half silently (flag 35) and
 * drops exactly one Network Interface item from the lower half position.
 */
public class NetworkInterfaceBlock extends BaseEntityBlock {

    public static final MapCodec<NetworkInterfaceBlock> CODEC =
            simpleCodec(NetworkInterfaceBlock::new);

    @Override
    public MapCodec<NetworkInterfaceBlock> codec() {
        return CODEC;
    }

    public NetworkInterfaceBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
    }

    // -------------------------------------------------------------------------
    // Blockstate
    // -------------------------------------------------------------------------

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DOUBLE_BLOCK_HALF);
    }

    // -------------------------------------------------------------------------
    // Placement
    // -------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        // Cancel placement if the block above cannot be replaced
        if (!level.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return null;
        }
        return defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            level.setBlockAndUpdate(pos.above(),
                    defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        }
    }

    // -------------------------------------------------------------------------
    // Removal
    // -------------------------------------------------------------------------

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.UPPER) {
                BlockPos lowerPos = pos.below();
                BlockState lowerState = level.getBlockState(lowerPos);
                if (lowerState.getBlock() == this
                        && lowerState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    // Drop the item at lower pos, then remove lower (which removes upper too)
                    Block.popResource(level, lowerPos,
                            new ItemStack(Registration.NETWORK_INTERFACE_ITEM.get()));
                    level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 35);
                    return state;
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean movedByPiston) {
        if (newState.getBlock() == this) {
            // Block replaced by the same block type — just do normal cleanup
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }

        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);

        if (otherState.getBlock() == this) {
            // Remove the other half silently (no drops, no cascading removal events)
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // -------------------------------------------------------------------------
    // Block entity
    // -------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                ? new NetworkInterfaceBlockEntity(pos, state)
                : null; // upper half has no block entity
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Delegate to the lower half's block entity
        BlockPos lowerPos = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                ? pos : pos.below();

        if (level.getBlockEntity(lowerPos) instanceof MenuProvider mp) {
            player.openMenu(mp, buf -> buf.writeBlockPos(lowerPos));
        }
        return InteractionResult.SUCCESS;
    }
}
