package net.bobofraggins.tremendousstorage.glamping;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A two-block-tall, unbreakable door that teleports the player back to wherever
 * they were when they entered the Glamping Dimension.
 *
 * <p>Placed by {@link GlampingPortalBlock} on the south wall of every new camp.
 * The upper and lower halves share this single block type, distinguished by the
 * {@link #HALF} property. Clicking either half triggers the teleport.
 */
public class TentDoorBlock extends Block {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public TentDoorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return InteractionResult.PASS;

        GlampingWorldData data = GlampingWorldData.getOrCreate(server);
        GlampingWorldData.ReturnTarget target = data.getReturnTarget(serverPlayer.getUUID());

        if (target == null) {
            // No stored return — fall back to overworld spawn.
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            serverPlayer.teleportTo(
                    overworld,
                    spawn.getX() + 0.5,
                    spawn.getY(),
                    spawn.getZ() + 0.5,
                    Set.of(),
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot());
        } else {
            ServerLevel returnLevel = server.getLevel(target.dimension());
            if (returnLevel != null) {
                serverPlayer.teleportTo(
                        returnLevel, target.x(), target.y(), target.z(), Set.of(), target.yRot(), target.xRot());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
