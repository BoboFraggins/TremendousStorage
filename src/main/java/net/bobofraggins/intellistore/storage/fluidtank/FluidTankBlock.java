package net.bobofraggins.intellistore.storage.fluidtank;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
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
public class FluidTankBlock extends BaseEntityBlock implements NetworkConnector {

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
        // MODEL renders the block model (base slab + glass jar); BESR handles fluid fill + stubs.
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private static final int BOTTLE_MB = 250;

    /**
     * Right-click with a fluid container item (bucket or glass bottle) to fill/drain the tank.
     *
     * <p>Buckets and modded fluid containers are handled by
     * {@link FluidUtil#interactWithFluidHandler} (1000 mB per interaction).
     *
     * <p>Glass bottles and water bottles are handled manually (250 mB per interaction):
     * <ul>
     *   <li>Empty glass bottle + tank has ≥ 250 mB water → give water bottle, drain tank.
     *   <li>Water bottle + tank accepts water and has ≥ 250 mB free → give empty bottle, fill tank.
     * </ul>
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        // --- Bottle handling (250 mB water only) ---
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be))
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            long drained = be.extract(BOTTLE_MB, true).getAmount();
            if (drained >= BOTTLE_MB) {
                be.extract(BOTTLE_MB, false);
                ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, waterBottle));
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be))
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                FluidStack water = new FluidStack(Fluids.WATER, BOTTLE_MB);
                long inserted = be.insert(water, BOTTLE_MB, true);
                if (inserted >= BOTTLE_MB) {
                    be.insert(water, BOTTLE_MB, false);
                    player.setItemInHand(
                            hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // --- Bucket / modded fluid container handling (via capability) ---
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, null, null);
        if (handler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        boolean success = FluidUtil.interactWithFluidHandler(player, hand, handler);
        return success ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Right-click with empty hand → open tank settings screen. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be)) return InteractionResult.FAIL;
        player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
