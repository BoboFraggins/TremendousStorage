package net.bobofraggins.tremendousstorage.glamping.skyblock;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * Writes the sky block's faces into the depth buffer only, with no colour output.
 *
 * <p>Because the skybox background is drawn before terrain, any pixel covered by this block
 * retains the raw sky colour while everything geometrically behind the block is hidden by the
 * depth value. The block model is empty (no quads) so the chunk renderer contributes nothing;
 * the block's solidity still causes the chunk builder to cull adjacent-block faces toward it,
 * preventing those faces from contaminating the framebuffer at sky-block pixels.
 */
public class SkyBlockRenderer implements BlockEntityRenderer<SkyBlockEntity> {

    private static final RenderType DEPTH_ONLY = RenderType.create(
            "tremendousstorage:sky_block_depth",
            DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_SHADER)
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(false, true))
                    .createCompositeState(false));

    public SkyBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            SkyBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        VertexConsumer vc = bufferSource.getBuffer(DEPTH_ONLY);
        Matrix4f m = poseStack.last().pose();
        // Six faces of the unit cube. Winding is CCW viewed from outside.
        // Bottom (y=0)
        vc.addVertex(m, 0, 0, 0);
        vc.addVertex(m, 0, 0, 1);
        vc.addVertex(m, 1, 0, 1);
        vc.addVertex(m, 1, 0, 0);
        // Top (y=1)
        vc.addVertex(m, 0, 1, 0);
        vc.addVertex(m, 1, 1, 0);
        vc.addVertex(m, 1, 1, 1);
        vc.addVertex(m, 0, 1, 1);
        // North (z=0)
        vc.addVertex(m, 0, 0, 0);
        vc.addVertex(m, 1, 0, 0);
        vc.addVertex(m, 1, 1, 0);
        vc.addVertex(m, 0, 1, 0);
        // South (z=1)
        vc.addVertex(m, 0, 0, 1);
        vc.addVertex(m, 0, 1, 1);
        vc.addVertex(m, 1, 1, 1);
        vc.addVertex(m, 1, 0, 1);
        // West (x=0)
        vc.addVertex(m, 0, 0, 0);
        vc.addVertex(m, 0, 1, 0);
        vc.addVertex(m, 0, 1, 1);
        vc.addVertex(m, 0, 0, 1);
        // East (x=1)
        vc.addVertex(m, 1, 0, 0);
        vc.addVertex(m, 1, 0, 1);
        vc.addVertex(m, 1, 1, 1);
        vc.addVertex(m, 1, 1, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(SkyBlockEntity entity) {
        return true;
    }
}
