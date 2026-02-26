package net.bobofraggins.intellistore.junkdrawer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Junk Drawer block — a plain cube that stores up to 32,768 of a single item type.
 *
 * <p>There is no player-facing UI. All item movement is via hoppers, pipes, or any mod that
 * reads the {@link net.neoforged.neoforge.items.IItemHandler} capability. The block locks to
 * the first item type inserted and stays locked (even at count 0) until cleared with
 * Whiteout Tape in the crafting grid.
 */
public class JunkDrawerBlock extends BaseEntityBlock {

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
}
