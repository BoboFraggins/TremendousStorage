package net.bobofraggins.intellistore.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.intellistore.shared.tank.AbstractTankRenderer;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the Network Interface block as a fluid tank containing an animated floating brain.
 *
 * <p>The static tank shell (octagonal cage) is rendered by the block model
 * ({@code models/block/network_interface.json}). This BESR handles:
 * <ul>
 *   <li>Water fill at 90% (octagonal prism, translucent)
 *   <li>Tube-connector stubs (via {@link AbstractTankRenderer})
 *   <li>Animated floating 3-D brain (bobbing + rotating, rendered from {@code block/brain_3d})
 * </ul>
 */
public class NetworkInterfaceRenderer extends AbstractTankRenderer<NetworkInterfaceBlockEntity> {

    private static final float FILL_FRAC = 0.9f;

    // Brain model center in 0-1 block space (bounding-box midpoint of brain_3d.json elements).
    private static final float BRAIN_CX = 6.95f / 16f;
    private static final float BRAIN_CY = 9.05f / 16f;
    private static final float BRAIN_CZ = 8.00f / 16f;

    static final ModelResourceLocation BRAIN_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("intellistore", "block/brain_3d"));

    public NetworkInterfaceRenderer(BlockEntityRendererProvider.Context ctx) {}

    // -------------------------------------------------------------------------
    // Fill — water at 90%
    // -------------------------------------------------------------------------

    @Override
    protected void renderFill(
            NetworkInterfaceBlockEntity be,
            Matrix4f mat,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Fluids.WATER);
        var fluidSprite = sprite(ext.getStillTexture(water));

        int tint = ext.getTintColor(water);
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 160;
        fa = fa * 2 / 3;

        float fillTop = FLUID_FLOOR + FILL_FRAC * FLUID_H;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        FluidTankRenderer.renderOctagonalPrism(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                packedLight,
                packedOverlay,
                fluidSprite.getU0(),
                fluidSprite.getV0(),
                fluidSprite.getU1(),
                Mth.lerp(FILL_FRAC, fluidSprite.getV0(), fluidSprite.getV1()),
                fluidSprite.getU0(),
                fluidSprite.getU1(),
                fluidSprite.getV0(),
                fluidSprite.getV1(),
                fillTop);
    }

    // -------------------------------------------------------------------------
    // Brain animation — no tube stubs on the NI
    // -------------------------------------------------------------------------

    @Override
    public void render(
            NetworkInterfaceBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        // Render fill only (skip AbstractTankRenderer's tube-stub logic).
        poseStack.pushPose();
        renderFill(be, poseStack.last().pose(), bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (be.getLevel() == null) return;

        double time = (be.getLevel().getGameTime() + partialTick) / 20.0;
        float bob = (float) Math.sin(time * Math.PI * 0.25) * 0.04f;

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f + bob, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90f));
        poseStack.translate(-BRAIN_CX, -BRAIN_CY, -BRAIN_CZ);

        renderBrain(be.getLevel(), be.getBlockState(), bufferSource, poseStack, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static void renderBrain(
            Level level,
            BlockState blockState,
            MultiBufferSource bufferSource,
            PoseStack poseStack,
            int packedLight,
            int packedOverlay) {

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(BRAIN_MODEL);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.cutout());
        RandomSource random = RandomSource.create();
        PoseStack.Pose pose = poseStack.last();

        for (var dir : net.minecraft.core.Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.cutout())) {
                float shade = level.getShade(dir, quad.isShade());
                vc.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.cutout())) {
            float shade = level.getShade(quad.getDirection(), quad.isShade());
            vc.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(NetworkInterfaceBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(NetworkInterfaceBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0);
    }
}
