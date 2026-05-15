package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BarrelBlock extends BaseEntityBlock implements NetworkConnector {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
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

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, Registration.BARREL_BE_TYPE.get(), BarrelBlockEntity::serverTick);
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
    protected InteractionResult useItemOn(
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
                if (be.hasCompactingUpgrade()) {
                    int slot = getCompactingSlot(hit, facing);
                    handleCompactingItemOn(be, stack, player, slot);
                } else {
                    insertHeld(be, stack, player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
                player.openMenu(be, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeBoolean(be.hasPullerUpgrade());
                });
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void insertHeld(BarrelBlockEntity be, ItemStack stack, Player player) {
        int original = stack.getCount();
        long remainder = be.insert(stack, original, false);
        if (!player.isCreative()) {
            int consumed = original - (int) Math.min(remainder, original);
            stack.shrink(consumed);
        }
    }

    // ── Compacting barrel helpers ──────────────────────────────────────────────

    /**
     * Maps a face hit to one of the three compacting slots (0, 1, 2).
     * In face-pixel space (y=0 at top, inner area x=2..13, y=2..13):
     *   slot 1 = top strip  x=2..13, y=2..5
     *   slot 0 = bottom-left  x=2..5,  y=10..13
     *   slot 2 = bottom-right x=10..13, y=10..13
     * Gap areas fall through to the nearest slot.
     */
    static int getCompactingSlot(BlockHitResult hit, Direction facing) {
        Vec3 p = hit.getLocation();
        BlockPos bp = hit.getBlockPos();
        double lx, ly;
        switch (facing) {
            case NORTH -> {
                lx = p.x - bp.getX();
                ly = p.y - bp.getY();
            }
            case SOUTH -> {
                lx = 1.0 - (p.x - bp.getX());
                ly = p.y - bp.getY();
            }
            case EAST -> {
                lx = p.z - bp.getZ();
                ly = p.y - bp.getY();
            }
            case WEST -> {
                lx = 1.0 - (p.z - bp.getZ());
                ly = p.y - bp.getY();
            }
            default -> {
                return 1;
            }
        }
        int px = (int) Math.max(0, Math.min(15, lx * 16));
        int py = (int) Math.max(0, Math.min(15, (1.0 - ly) * 16)); // y=0 at top
        // Top strip → slot 1; bottom half split left/right → slots 2/0; gap falls through
        if (py <= 7) return 1;
        return px < 8 ? 2 : 0;
    }

    private static void handleCompactingItemOn(BarrelBlockEntity be, ItemStack stack, Player player, int slot) {
        if (stack.isEmpty()) return;
        if (!be.isLocked()) {
            be.lockCompactingChain(stack, slot);
        }
        if (!be.isLocked()) return;
        // Locked (just now or previously): try to insert the held item into whichever slot matches
        var handler = new CompactingBarrelItemHandler(be);
        for (int s = 0; s < 3; s++) {
            if (handler.isItemValid(s, stack)) {
                ItemStack remainder = handler.insertItem(s, stack.copy(), false);
                if (!player.isCreative()) {
                    int consumed = stack.getCount() - (remainder.isEmpty() ? 0 : remainder.getCount());
                    stack.shrink(consumed);
                }
                return;
            }
        }
    }

    static void extractCompactingSlot(BarrelBlockEntity be, Player player, int slot, int amount) {
        if (!be.isLocked()) return;
        var handler = new CompactingBarrelItemHandler(be);
        ItemStack extracted = handler.extractItem(slot, amount, false);
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
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @javax.annotation.Nullable net.minecraft.world.level.redstone.Orientation orientation,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
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
