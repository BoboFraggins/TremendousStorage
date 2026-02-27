package net.bobofraggins.intellistore.fluidtank;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The Fluid Tank block — stores up to {@link FluidTankBlockEntity#CAPACITY} mB of a single fluid type.
 *
 * <p>The front face (the direction the player faces when placing) shows a 12×12 transparent
 * window. When the tank contains a fluid, the fluid's texture is rendered there by
 * {@link FluidTankRenderer}.
 *
 * <p>Right-clicking with a fluid container item (e.g. a bucket) fills or drains the tank.
 * All other fluid movement is via the {@link FluidTankFluidHandler} IFluidHandler capability.
 */
public class FluidTankBlock extends BaseEntityBlock {

    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public FluidTankBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<FluidTankBlock> codec() {
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
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED required for the BlockEntityRenderer to be called.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * Right-click with a fluid container item (full or empty bucket, etc.) to fill/drain the tank.
     *
     * <p>{@link FluidUtil#interactWithFluidHandler} handles both directions automatically:
     * an empty container draws from the tank; a full container pours into it. The resulting
     * item (filled/emptied container) is placed back in the player's hand or stowed.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, null, null);
        if (handler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        boolean success = FluidUtil.interactWithFluidHandler(player, hand, handler);
        return success ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
