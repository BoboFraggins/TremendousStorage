package net.bobofraggins.tremendousstorage.glamping;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Glamping Portal block.
 *
 * <p>Right-clicking teleports the player into the Glamping Dimension. On first use a fresh
 * 16×16×16 camp is carved, a {@link TentDoorBlock} is placed on the south wall, and the
 * block entity records the camp origin so every subsequent use arrives at the same spot.
 *
 * <p>The player's entry location is saved to {@link GlampingWorldData} so the tent door
 * can return them to exactly where they came from.
 *
 * <p>If used from inside the Glamping Dimension the portal falls back to sending the player
 * to the overworld shared spawn (the tent door is the intended exit).
 */
public class GlampingPortalBlock extends BaseEntityBlock {

    public static final MapCodec<GlampingPortalBlock> CODEC = simpleCodec(GlampingPortalBlock::new);

    @Override
    public MapCodec<GlampingPortalBlock> codec() {
        return CODEC;
    }

    public GlampingPortalBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlampingPortalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (level.dimension() == GlampingDimension.KEY) {
            teleportToOverworld(serverPlayer);
        } else {
            teleportToGlamping(serverPlayer, (ServerLevel) level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // -------------------------------------------------------------------------

    private void teleportToGlamping(ServerPlayer player, ServerLevel currentLevel, BlockPos portalPos) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel glampingLevel = server.getLevel(GlampingDimension.KEY);
        if (glampingLevel == null) return;

        BlockEntity be = currentLevel.getBlockEntity(portalPos);
        if (!(be instanceof GlampingPortalBlockEntity portalBE)) return;

        // Store where the player came from before moving them.
        GlampingWorldData.getOrCreate(server)
                .storeReturnTarget(
                        player.getUUID(),
                        currentLevel.dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot());

        BlockPos campOrigin;
        if (!portalBE.hasCamp()) {
            campOrigin = GlampingWorldData.getOrCreate(server).allocateNewCamp();
            portalBE.setCampOrigin(campOrigin);
            carveSpace(glampingLevel, campOrigin);
            placeTentDoor(glampingLevel, campOrigin);
        } else {
            campOrigin = portalBE.getCampOrigin();
        }

        // Place the player at the centre of the carved space, standing on the canvas floor.
        double tx = campOrigin.getX() + GlampingDimension.CAMP_SIZE / 2.0;
        double ty = campOrigin.getY();
        double tz = campOrigin.getZ() + GlampingDimension.CAMP_SIZE / 2.0;
        player.teleportTo(glampingLevel, tx, ty, tz, Set.of(), player.getYRot(), player.getXRot());
    }

    private void teleportToOverworld(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(
                overworld,
                spawn.getX() + 0.5,
                spawn.getY(),
                spawn.getZ() + 0.5,
                Set.of(),
                player.getYRot(),
                player.getXRot());
    }

    /**
     * Removes a 16×16×16 cube of blocks starting at {@code origin} (inclusive).
     * The canvas block at Y = origin.y - 1 is left intact as the floor.
     */
    private void carveSpace(ServerLevel level, BlockPos origin) {
        int size = GlampingDimension.CAMP_SIZE;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * Places a {@link TentDoorBlock} centered on the south wall of the carved space.
     *
     * <p>The door sits at X = origin.x + 7, Z = origin.z + 15 (the southernmost air column),
     * with the lower half at origin.y and the upper half one block above.
     */
    private void placeTentDoor(ServerLevel level, BlockPos origin) {
        BlockState lower = GlampingRegistration.TENT_DOOR
                .get()
                .defaultBlockState()
                .setValue(TentDoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = GlampingRegistration.TENT_DOOR
                .get()
                .defaultBlockState()
                .setValue(TentDoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlock(origin.offset(7, 0, 15), lower, 3);
        level.setBlock(origin.offset(7, 1, 15), upper, 3);
    }
}
