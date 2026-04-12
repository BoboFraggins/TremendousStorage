package net.bobofraggins.tremendousstorage.glamping;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A two-block-deep tent, placed like a vanilla bed.
 *
 * <p>The block the player targets becomes FOOT; the block one step in the facing
 * direction becomes HEAD. Only the FOOT half has a {@link TentBlockEntity}.
 *
 * <p>On first right-click, the tent's name is used as a random seed to derive a
 * unique X,Z location in the Glamping Dimension. A 16×16×16 camp is carved there
 * (if not already claimed) and the location is stored in the block entity for all
 * future uses.
 */
public class TentBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<TentBlock> CODEC = simpleCodec(TentBlock::new);

    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    /** Grid range in slots (±). With CAMP_SPACING=32 this gives ±16 000 blocks. */
    private static final int CAMP_SLOT_RANGE = 500;

    public TentBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, BedPart.FOOT));
    }

    @Override
    public MapCodec<TentBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    // -------------------------------------------------------------------------
    // Block entity (FOOT only)
    // -------------------------------------------------------------------------

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.FOOT ? new TentBlockEntity(pos, state) : null;
    }

    // -------------------------------------------------------------------------
    // Placement — copy name from item into the FOOT block entity
    // -------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection();
        BlockPos footPos = ctx.getClickedPos();
        BlockPos headPos = footPos.relative(facing);
        Level level = ctx.getLevel();
        if (!level.getBlockState(headPos).canBeReplaced(ctx) || !level.isInWorldBounds(headPos)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.FOOT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;
        BlockPos headPos = pos.relative(state.getValue(FACING));
        level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
        level.blockUpdated(pos, Blocks.AIR);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);

        // Copy the item's custom name into the FOOT block entity so it survives the
        // place/break cycle.
        if (level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            be.setTentName(name != null ? name.getString() : null);
        }
    }

    // -------------------------------------------------------------------------
    // Drops — restore the custom name onto the dropped item
    // -------------------------------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (state.getValue(PART) != BedPart.FOOT) return drops;
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof TentBlockEntity tentBE) || tentBE.getTentName() == null) return drops;
        for (ItemStack drop : drops) {
            if (drop.is(this.asItem())) {
                drop.set(
                        DataComponents.CUSTOM_NAME,
                        Component.literal(tentBE.getTentName()).withStyle(s -> s.withItalic(false)));
            }
        }
        return drops;
    }

    // -------------------------------------------------------------------------
    // Right-click: derive camp from seed (first use) then teleport
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return InteractionResult.PASS;

        ServerLevel glampingLevel = server.getLevel(GlampingDimension.KEY);
        if (glampingLevel == null) return InteractionResult.PASS;

        // Always work from the FOOT's block entity regardless of which half was clicked.
        BlockPos footPos = state.getValue(PART) == BedPart.FOOT
                ? pos
                : pos.relative(state.getValue(FACING).getOpposite());
        if (!(level.getBlockEntity(footPos) instanceof TentBlockEntity be)) return InteractionResult.PASS;

        // Store where the player is now so the tent door can bring them back.
        GlampingWorldData data = GlampingWorldData.getOrCreate(server);
        data.storeReturnTarget(
                serverPlayer.getUUID(),
                level.dimension(),
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                serverPlayer.getYRot(),
                serverPlayer.getXRot());

        BlockPos campOrigin;
        if (be.hasCamp()) {
            campOrigin = be.getCampOrigin();
        } else {
            // First use — derive the camp location from the name seed.
            campOrigin = deriveOrigin(be.getTentName());
            be.setCampOrigin(campOrigin);
            if (!data.isClaimed(campOrigin)) {
                data.claimCamp(campOrigin);
                carveSpace(glampingLevel, campOrigin);
            }
            // If already claimed the space already exists — just land there.
        }

        double tx = campOrigin.getX() + GlampingDimension.CAMP_SIZE / 2.0;
        double ty = campOrigin.getY();
        double tz = campOrigin.getZ() + GlampingDimension.CAMP_SIZE / 2.0;
        serverPlayer.teleportTo(glampingLevel, tx, ty, tz, Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot());

        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Breaking
    // -------------------------------------------------------------------------

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (neighborState.is(this)) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        Direction facing = state.getValue(FACING);
        BedPart part = state.getValue(PART);
        Direction toPartner = (part == BedPart.FOOT) ? facing : facing.getOpposite();
        if (direction == toPartner) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            removePartnerBlock(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removePartnerBlock(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BedPart part = state.getValue(PART);
        BlockPos partnerPos = (part == BedPart.FOOT) ? pos.relative(facing) : pos.relative(facing.getOpposite());
        BlockState partnerState = level.getBlockState(partnerPos);
        if (partnerState.is(this)) {
            level.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Derives a deterministic camp origin from the tent's name.
     *
     * <p>The name string is hashed to a long seed, which drives a {@link Random}
     * to pick a grid slot within ±{@value #CAMP_SLOT_RANGE} slots on each axis.
     * Snapping to the {@link GlampingDimension#CAMP_SPACING} grid ensures camps
     * never physically overlap regardless of how many are allocated.
     */
    private static BlockPos deriveOrigin(@Nullable String name) {
        String s = (name != null && !name.isEmpty()) ? name : "tent";
        long seed = s.chars().reduce(0, (acc, c) -> acc * 31 + c);
        Random rng = new Random(seed);
        int range = CAMP_SLOT_RANGE * 2 + 1;
        int slotX = rng.nextInt(range) - CAMP_SLOT_RANGE;
        int slotZ = rng.nextInt(range) - CAMP_SLOT_RANGE;
        return new BlockPos(
                slotX * GlampingDimension.CAMP_SPACING,
                GlampingDimension.CAMP_BOTTOM_Y,
                slotZ * GlampingDimension.CAMP_SPACING);
    }

    private static void carveSpace(ServerLevel level, BlockPos origin) {
        int size = GlampingDimension.CAMP_SIZE;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
