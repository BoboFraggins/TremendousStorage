package net.bobofraggins.tremendousstorage.glamping;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
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
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);

        if (!(level.getBlockEntity(pos) instanceof TentBlockEntity be)) return;

        Component name = stack.get(DataComponents.CUSTOM_NAME);
        be.setTentName(name != null ? name.getString() : null);

        // Derive and carve the camp at placement time so the location is fixed
        // regardless of whether the player renames the tent before entering it.
        MinecraftServer server = ((ServerLevel) level).getServer();
        if (server == null) return;
        ServerLevel glampingLevel = server.getLevel(GlampingDimension.KEY);
        if (glampingLevel == null) return;
        BlockPos campOrigin = deriveOrigin(be.getTentName());
        be.setCampOrigin(campOrigin);
        GlampingWorldData data = GlampingWorldData.getOrCreate(server);
        if (!data.isClaimed(campOrigin)) {
            data.claimCamp(campOrigin);
            carveSpace(glampingLevel, campOrigin);
        }
    }

    // -------------------------------------------------------------------------
    // Drops — restore the custom name onto the dropped item
    // -------------------------------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(PART) != BedPart.FOOT) return List.of();
        List<ItemStack> drops = super.getDrops(state, params);
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

        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) serverPlayer.level()).getServer();
        if (server == null) return InteractionResult.PASS;

        ServerLevel glampingLevel = server.getLevel(GlampingDimension.KEY);
        if (glampingLevel == null) return InteractionResult.PASS;

        // Always work from the FOOT's block entity regardless of which half was clicked.
        BlockPos footPos = state.getValue(PART) == BedPart.FOOT
                ? pos
                : pos.relative(state.getValue(FACING).getOpposite());
        if (!(level.getBlockEntity(footPos) instanceof TentBlockEntity be)) return InteractionResult.PASS;

        // Store where the player is now so the tent door can bring them back.
        // Use the tent's facing to compute the exit yRot (face away from the tent on return).
        Direction tentFacing = state.getValue(FACING);
        float exitYRot = directionToYRot(tentFacing.getOpposite());
        GlampingWorldData data = GlampingWorldData.getOrCreate(server);
        data.storeReturnTarget(
                serverPlayer.getUUID(),
                level.dimension(),
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                exitYRot,
                0f);

        BlockPos campOrigin;
        if (be.hasCamp()) {
            campOrigin = be.getCampOrigin();
        } else {
            // Fallback: camp should have been carved at placement time, but derive it
            // now if somehow the block entity was placed without going through setPlacedBy.
            campOrigin = deriveOrigin(be.getTentName());
            be.setCampOrigin(campOrigin);
            if (!data.isClaimed(campOrigin)) {
                data.claimCamp(campOrigin);
                carveSpace(glampingLevel, campOrigin);
            }
        }

        // Land the player centered in the block directly inside the tent door.
        // Door is at camp-relative (size/2, 0, size-1); one block inside is (size/2, 0, size-2).
        double tx = campOrigin.getX() + GlampingDimension.CAMP_SIZE / 2 + 0.5;
        double ty = campOrigin.getY();
        double tz = campOrigin.getZ() + GlampingDimension.CAMP_SIZE - 2 + 0.5;
        // Face north (away from the TentDoor, which is always on the south wall).
        serverPlayer.teleportTo(glampingLevel, tx, ty, tz, Set.<Relative>of(), 180f, 0f, false);

        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Breaking
    // -------------------------------------------------------------------------

    @Override
    public BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (neighborState.is(this)) {
            return super.updateShape(
                    state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        }
        Direction facing = state.getValue(FACING);
        BedPart part = state.getValue(PART);
        Direction toPartner = (part == BedPart.FOOT) ? facing : facing.getOpposite();
        if (direction == toPartner) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
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

    private static float directionToYRot(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    private static final Identifier CAMP_STRUCTURE =
            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "glamping_camp");

    /**
     * The structure's (0,0,0) corner is offset from campOrigin by this amount so that the
     * template tent door aligns with the camp's expected door position.
     *
     * <p>Template door (lower half) is at relative position (14,6,25) within the 32×14×32 structure.
     * Camp door is at campOrigin.offset(8, 0, 15), so structure origin =
     * campOrigin.offset(8-14, 0-6, 15-25) = campOrigin.offset(-6, -6, -10).
     */
    private static final BlockPos STRUCTURE_OFFSET = new BlockPos(-6, -6, -10);

    private static void carveSpace(ServerLevel level, BlockPos campOrigin) {
        Optional<StructureTemplate> template = level.getStructureManager().get(CAMP_STRUCTURE);
        if (template.isPresent()) {
            BlockPos structureOrigin = campOrigin.offset(STRUCTURE_OFFSET);
            template.get()
                    .placeInWorld(
                            level,
                            structureOrigin,
                            structureOrigin,
                            new StructurePlaceSettings(),
                            level.getRandom(),
                            Block.UPDATE_ALL);
        } else {
            // Fallback: carve a plain 16×16×16 space with a tent door.
            int size = GlampingDimension.CAMP_SIZE;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    for (int dz = 0; dz < size; dz++) {
                        level.setBlock(campOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
            BlockPos doorBase = campOrigin.offset(size / 2, 0, size - 1);
            BlockState doorState = GlampingRegistration.TENT_DOOR.get().defaultBlockState();
            level.setBlock(doorBase, doorState.setValue(TentDoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
            level.setBlock(
                    doorBase.above(), doorState.setValue(TentDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }
}
