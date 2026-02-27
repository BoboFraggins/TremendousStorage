package net.bobofraggins.intellistore.bulkstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.bobofraggins.intellistore.tube.NetworkConnector;

/**
 * The Bulk Storage Container block — stores up to {@value BulkStorageContainerBlockEntity#CAPACITY}
 * items in a shared pool across any number of distinct item types.
 *
 * <p>Accepts only items that Manila Folders accept: non-damageable items with default component
 * data (plain stackable items). This is the precise complement of the Junk Drawer.
 * There is no locking — any qualifying item may be freely added or removed at any time.
 *
 * <p>There is no player-facing UI. All item movement is via hoppers, pipes, or any mod that
 * reads the {@link net.neoforged.neoforge.items.IItemHandler} capability.
 */
public class BulkStorageContainerBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<BulkStorageContainerBlock> CODEC = simpleCodec(BulkStorageContainerBlock::new);

    @Override
    public MapCodec<BulkStorageContainerBlock> codec() {
        return CODEC;
    }

    public BulkStorageContainerBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BulkStorageContainerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
}
