package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BarrelBlock extends BaseEntityBlock implements NetworkConnector {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StorageTier> TIER = EnumProperty.create("tier", StorageTier.class);

    public static final MapCodec<BarrelBlock> CODEC = simpleCodec(BarrelBlock::new);

    public BarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TIER, StorageTier.WOOD));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(TIER, StorageTier.WOOD);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    /**
     * Returns true if {@code hit} falls within the 12×12 item-display area on the barrel's
     * front face (2-pixel inset from each edge on the 16×16 face texture).
     * Package-visible so StorageUpgradeItem and BarrelClientEvents can reuse it.
     */
    public static boolean isInItemArea(BlockHitResult hit, Direction facing) {
        Vec3 loc = hit.getLocation();
        BlockPos bp = hit.getBlockPos();
        double lx = loc.x - bp.getX();
        double ly = loc.y - bp.getY();
        double lz = loc.z - bp.getZ();
        final double lo = 2.0 / 16.0;
        final double hi = 14.0 / 16.0;
        return switch (facing.getAxis()) {
            case X -> ly >= lo && ly <= hi && lz >= lo && lz <= hi;
            case Z -> lx >= lo && lx <= hi && ly >= lo && ly <= hi;
            default -> false;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection() == facing && isInItemArea(hit, facing)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
                extractStack(be, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection() == facing && isInItemArea(hit, facing)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
                extractStack(be, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        // Barrel-click area: open UI
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
                player.openMenu(be, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void extractStack(BarrelBlockEntity be, Player player) {
        if (!be.isLocked() || be.getCount() == 0) return;
        int amount = be.getStoredItem().getMaxStackSize();
        ItemStack extracted = be.extract(amount, false);
        if (!extracted.isEmpty() && !player.addItem(extracted)) {
            player.drop(extracted, false);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
                level.updateNeighbourForOutputSignal(pos, this);
                be.drops();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
            long count = be.getCount();
            if (count == 0) return 0;
            long cap = be.getCapacity();
            return 1 + (int) (14L * count / cap);
        }
        return 0;
    }

    @Override
    public void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
            level.invalidateCapabilities(pos);
        }
    }

    public static void setTierBlockState(Level level, BlockPos pos, StorageTier tier) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(TIER) && state.getValue(TIER) != tier) {
            level.setBlock(pos, state.setValue(TIER, tier), 2);
        }
    }
}
