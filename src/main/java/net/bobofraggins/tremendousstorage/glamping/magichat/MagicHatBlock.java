package net.bobofraggins.tremendousstorage.glamping.magichat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The placeable Magic Hat block. Shape matches the blockbench model geometry. */
public class MagicHatBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 1, 13), // brim
            Block.box(5, 1, 5, 11, 8, 11) // top
            );

    public MagicHatBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
