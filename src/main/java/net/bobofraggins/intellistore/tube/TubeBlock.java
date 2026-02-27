package net.bobofraggins.intellistore.tube;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.StorageInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * A pipe block that visually connects to adjacent same-color tubes and to any block exposing
 * an {@code IItemHandler} capability (storage blocks). Tubes have a 4×4 pixel cross-section
 * (1/4 × 1/4 block) and extend arms toward connected faces.
 *
 * <p>Color is stored as a final field (one {@code TubeBlock} instance per {@link DyeColor}).
 * Different-colored tubes do not connect to each other.
 *
 * <p>Each tube face may hold a "Storage Interface" attachment (stored in the block entity).
 * Right-clicking an attachment face opens its priority screen; right-clicking an open face
 * installs a new attachment.
 */
public class TubeBlock extends BaseEntityBlock {

    // -------------------------------------------------------------------------
    // Codec
    // -------------------------------------------------------------------------

    public static final MapCodec<TubeBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            DyeColor.CODEC.fieldOf("color").forGetter(TubeBlock::getColor),
                            propertiesCodec())
                    .apply(instance, TubeBlock::new));

    @Override
    public MapCodec<TubeBlock> codec() {
        return CODEC;
    }

    // -------------------------------------------------------------------------
    // Blockstate properties
    // -------------------------------------------------------------------------

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    /** Indexed by {@link Direction#ordinal()} — same order as {@code Direction.values()}. */
    public static final BooleanProperty[] DIR_PROPS = {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    };

    // -------------------------------------------------------------------------
    // VoxelShapes — 64-entry array indexed by 6-bit connection mask
    // -------------------------------------------------------------------------

    /** Core 4×4×4 pixels, centred. */
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    /** Arm from core to each face, one per Direction ordinal. */
    private static final VoxelShape[] ARM_SHAPES = {
        Block.box(6, 0, 6, 10, 6, 10),   // DOWN  (ordinal 0)
        Block.box(6, 10, 6, 10, 16, 10),  // UP    (ordinal 1)
        Block.box(6, 6, 0, 10, 10, 6),    // NORTH (ordinal 2)
        Block.box(6, 6, 10, 10, 10, 16),  // SOUTH (ordinal 3)
        Block.box(0, 6, 6, 6, 10, 10),    // WEST  (ordinal 4)
        Block.box(10, 6, 6, 16, 10, 10),  // EAST  (ordinal 5)
    };

    /** 64 pre-computed shapes, one per connection bitmask. */
    private final VoxelShape[] shapes;

    // -------------------------------------------------------------------------
    // Color
    // -------------------------------------------------------------------------

    private final DyeColor color;

    public DyeColor getColor() {
        return color;
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public TubeBlock(DyeColor color, Properties props) {
        super(props);
        this.color = color;
        registerDefaultState(buildDefaultState());
        this.shapes = buildShapes();
    }

    private BlockState buildDefaultState() {
        return stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false);
    }

    private VoxelShape[] buildShapes() {
        VoxelShape[] arr = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = CORE;
            for (int d = 0; d < 6; d++) {
                if ((mask & (1 << d)) != 0) {
                    s = Shapes.or(s, ARM_SHAPES[d]);
                }
            }
            arr[mask] = s;
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // Blockstate definition
    // -------------------------------------------------------------------------

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    // -------------------------------------------------------------------------
    // Shape
    // -------------------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[connectionMask(state)];
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        for (int d = 0; d < 6; d++) {
            if (state.getValue(DIR_PROPS[d])) mask |= (1 << d);
        }
        return mask;
    }

    // -------------------------------------------------------------------------
    // Placement and neighbor updates
    // -------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeState(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide()) {
            BlockState updated = computeState(state, level, pos);
            if (!updated.equals(state)) {
                level.setBlockAndUpdate(pos, updated);
            }
            // Clear the network cache when any neighbor changes (storage block added/removed,
            // or priority changed via sendBlockUpdated flags=3 from adjacent storage BEs).
            // Guard with hasNetworkCache() to prevent O(n²) cascade through large networks.
            if (level.getBlockEntity(pos) instanceof TubeBlockEntity be && be.hasNetworkCache()) {
                be.setChanged();
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(DIR_PROPS[direction.ordinal()],
                canConnectToState(neighborState, level, neighborPos, direction));
    }

    private BlockState computeState(BlockState current, LevelReader level, BlockPos pos) {
        BlockState s = current;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            s = s.setValue(DIR_PROPS[dir.ordinal()],
                    canConnectToState(neighborState, level, neighborPos, dir));
        }
        return s;
    }

    /**
     * Returns true if this tube should connect to the block at {@code neighborPos}.
     * Connects to same-color tubes, or to any block that exposes an IItemHandler capability.
     */
    private boolean canConnectToState(BlockState neighborState, LevelReader level,
            BlockPos neighborPos, Direction fromDir) {
        if (neighborState.getBlock() instanceof TubeBlock tb) {
            return tb.getColor() == this.color;
        }
        // Connect to any block with an IItemHandler (storage blocks, hoppers, etc.)
        if (level instanceof Level worldLevel) {
            var be = worldLevel.getBlockEntity(neighborPos);
            if (be != null) {
                var cap = worldLevel.getCapability(
                        Capabilities.ItemHandler.BLOCK, neighborPos, fromDir.getOpposite());
                return cap != null;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // -------------------------------------------------------------------------
    // Block entity
    // -------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeBlockEntity(pos, state);
    }

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    /**
     * Right-click with a Storage Interface item: install attachment on the clicked face
     * (if not already present) and consume one item from the stack.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof StorageInterfaceItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TubeBlockEntity be)) return ItemInteractionResult.FAIL;

        int faceIndex = hit.getDirection().ordinal();
        if (be.hasAttachment(faceIndex)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        be.setAttachment(faceIndex, true);
        if (!player.isCreative()) stack.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Right-click with empty hand: open the priority screen for an installed attachment.
     * Does nothing on faces without an attachment (installation now requires the item).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TubeBlockEntity be)) return InteractionResult.FAIL;

        int faceIndex = hit.getDirection().ordinal();
        if (!be.hasAttachment(faceIndex)) return InteractionResult.PASS;

        player.openMenu(
                new StorageInterfaceMenu.Provider(be, pos, faceIndex),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(faceIndex);
                });
        return InteractionResult.SUCCESS;
    }

    /**
     * Left-click (punch): if the hit point lands inside an attachment plate's AABB,
     * remove that attachment and drop one Storage Interface item.
     */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof TubeBlockEntity be)) return;

        Vec3 hit = player.pick(player.blockInteractionRange(), 1.0f, false).getLocation();
        Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());

        for (int i = 0; i < 6; i++) {
            if (!be.hasAttachment(i)) continue;
            if (attachmentAABB(i).contains(local)) {
                be.setAttachment(i, false);
                Block.popResource(level, pos, new ItemStack(Registration.STORAGE_INTERFACE.get()));
                return;
            }
        }
    }

    /**
     * When the tube is broken, drop one Storage Interface item per installed attachment
     * in addition to the tube block itself (handled by the loot table).
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity be, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, be, tool);
        if (be instanceof TubeBlockEntity tube) {
            for (int i = 0; i < 6; i++) {
                if (tube.hasAttachment(i)) {
                    Block.popResource(level, pos,
                            new ItemStack(Registration.STORAGE_INTERFACE.get()));
                }
            }
        }
    }

    /**
     * Returns the AABB (block-local [0..1] coords) for the attachment plate on face {@code i}.
     * Matches the geometry drawn by {@link TubeRenderer}: P_MIN=4/16, P_MAX=12/16, P_THICK=2/16.
     */
    private static AABB attachmentAABB(int i) {
        final double L = 4.0 / 16, H = 12.0 / 16, T = 2.0 / 16;
        return switch (i) {
            case 0 -> new AABB(L, 0,   L, H, T,   H); // DOWN
            case 1 -> new AABB(L, 1-T, L, H, 1,   H); // UP
            case 2 -> new AABB(L, L,   0, H, H,   T); // NORTH
            case 3 -> new AABB(L, L, 1-T, H, H,   1); // SOUTH
            case 4 -> new AABB(0, L,   L, T, H,   H); // WEST
            default-> new AABB(1-T, L, L, 1, H,   H); // EAST
        };
    }
}
