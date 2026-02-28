package net.bobofraggins.intellistore.storage.wirelesshub;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Wireless Hub block.
 *
 * <p>Right-clicking opens a 2-slot UI. The left slot accepts an unlinked Wireless SAT;
 * when placed there the hub reads the Network Interface position from the BFS scan and
 * records it into the item as a data component, then moves the item to the right slot
 * for retrieval. Only works when the network is valid.
 */
public class WirelessHubBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<WirelessHubBlock> CODEC = simpleCodec(WirelessHubBlock::new);

    @Override
    public MapCodec<WirelessHubBlock> codec() {
        return CODEC;
    }

    public WirelessHubBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessHubBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // All geometry (base, rods, arc) is drawn by WirelessHubRenderer; JSON provides particle only.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof MenuProvider mp) {
            player.openMenu(mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // Client-only ticker for the arc animation
        return level.isClientSide ? (lvl, pos, st, be) -> ((WirelessHubBlockEntity) be).clientTick() : null;
    }
}
