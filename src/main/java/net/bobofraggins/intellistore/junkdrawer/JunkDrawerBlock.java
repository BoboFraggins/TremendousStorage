package net.bobofraggins.intellistore.junkdrawer;

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
 * The Junk Drawer block — stores up to 32,768 individual items, one per slot.
 *
 * <p>Accepts only items that Manila Folders reject: damageable items (tools, armour, weapons)
 * and items with non-default component data (enchanted books, named items, potions, etc.).
 * There is no locking — any qualifying item may be freely added or removed at any time.
 *
 * <p>There is no player-facing UI. All item movement is via hoppers, pipes, or any mod that
 * reads the {@link net.neoforged.neoforge.items.IItemHandler} capability.
 */
public class JunkDrawerBlock extends BaseEntityBlock implements NetworkConnector {

    public static final MapCodec<JunkDrawerBlock> CODEC = simpleCodec(JunkDrawerBlock::new);

    @Override
    public MapCodec<JunkDrawerBlock> codec() {
        return CODEC;
    }

    public JunkDrawerBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JunkDrawerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Use the standard JSON cube model.
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
