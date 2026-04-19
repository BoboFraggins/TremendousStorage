package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.tank.AbstractTankRenderer;
import net.bobofraggins.tremendousstorage.storage.tank.TankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the Network Interface block as a fluid tank containing an animated floating brain.
 *
 * <p>The static tank shell (octagonal cage) is rendered by the block model
 * ({@code models/block/network_interface.json}). This BESR handles:
 * <ul>
 *   <li>Water fill at 90% (octagonal prism, translucent)
 *   <li>Animated floating Brain item (bobbing, facing matches placement direction)
 * </ul>
 */
public class NetworkInterfaceRenderer extends AbstractTankRenderer<NetworkInterfaceBlockEntity> {

    // Glass extends to y=16, so interior height is taller than the standard tank.
    private static final float NI_FLUID_H = 1.0f - FLUID_FLOOR;
    private static final float FILL_FRAC = 0.9f;

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
        fa /= 2;

        float fillTop = FLUID_FLOOR + FILL_FRAC * NI_FLUID_H;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        TankRenderer.renderOctagonalPrism(
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
        float rotY = (float) ((time * 10.0) % 360.0);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.XP.rotationDegrees(3f));

        ItemStack brainStack = new ItemStack(Registration.BRAIN.get());
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(brainStack, be.getLevel(), null, 0);
        Minecraft.getInstance()
                .getItemRenderer()
                .render(
                        brainStack,
                        ItemDisplayContext.FIXED,
                        false,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        model);

        poseStack.popPose();
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
