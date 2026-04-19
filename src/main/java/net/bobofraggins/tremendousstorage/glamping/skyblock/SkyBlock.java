package net.bobofraggins.tremendousstorage.glamping.skyblock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that renders only into the depth buffer — no colour is written to the framebuffer.
 *
 * <p>Because the sky/void background is drawn before terrain, any pixel covered by this
 * block retains the raw skybox colour while everything behind the block is hidden by the
 * depth value, producing a "window to the skybox" effect.
 *
 * <p>The depth-only rendering is done by {@link SkyBlockRenderer} (a BER), which has full
 * RenderType freedom outside the chunk buffer. The block model has no elements so the chunk
 * renderer contributes nothing; the block's solid shape still causes adjacent-block faces
 * toward it to be culled, keeping those faces out of the framebuffer.
 */
public class SkyBlock extends Block implements EntityBlock {

    public SkyBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkyBlockEntity(pos, state);
    }
}
