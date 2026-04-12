package net.bobofraggins.tremendousstorage.glamping.skyblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A block that renders only into the depth buffer — no colour is written to the framebuffer.
 *
 * <p>Because the sky/void background is drawn before terrain, any pixel covered by this
 * block retains the raw skybox colour while everything behind the block is hidden by the
 * depth value, producing a perfect "window to the skybox" effect with no other geometry
 * visible through it.
 *
 * <p>The depth-only {@link net.minecraft.client.renderer.RenderType} is registered in
 * {@link SkyBlockClientEvents} and referenced by the block model JSON.
 */
public class SkyBlock extends Block {

    public SkyBlock(BlockBehaviour.Properties props) {
        super(props);
    }
}
