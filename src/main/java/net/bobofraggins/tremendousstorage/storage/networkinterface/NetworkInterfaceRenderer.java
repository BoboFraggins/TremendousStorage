package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.tank.AbstractTankRenderer;
import net.bobofraggins.tremendousstorage.storage.tank.TankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the Network Interface block as a fluid tank containing an animated floating brain.
 *
 * <p>The static tank shell is rendered by the block model (which now inherits from
 * {@code models/block/tank.json}). This BESR handles:
 * <ul>
 *   <li>Positive Vibes fill at 95% (cube, translucent)
 *   <li>Animated floating Brain item (bobbing, facing matches placement direction)
 * </ul>
 */
public class NetworkInterfaceRenderer extends AbstractTankRenderer<NetworkInterfaceBlockEntity> {

    private static final float FILL_FRAC = 0.95f;

    public NetworkInterfaceRenderer(BlockEntityRendererProvider.Context ctx) {}

    // -------------------------------------------------------------------------
    // Fill — Positive Vibes at 95%
    // -------------------------------------------------------------------------

    @Override
    protected void renderFill(
            NetworkInterfaceBlockEntity be,
            Matrix4f mat,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        FluidStack vibes = new FluidStack(Registration.POSITIVE_VIBES_SOURCE.get(), 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.POSITIVE_VIBES_TYPE.get());
        var fluidSprite = sprite(ext.getStillTexture(vibes));

        int tint = ext.getTintColor(vibes);
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 160;
        fa /= 2;

        float fillTop = TankRenderer.TANK_FLUID_FLOOR + FILL_FRAC * TankRenderer.TANK_FLUID_H;
        float vB = Mth.lerp(FILL_FRAC, fluidSprite.getV0(), fluidSprite.getV1());

        int fluidLight =
                Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentItemSheet());
        TankRenderer.renderCubeFill(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                fluidSprite.getU0(),
                fluidSprite.getV0(),
                fluidSprite.getU1(),
                vB,
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

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        poseStack.pushPose();
        renderFill(be, poseStack.last().pose(), bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (level == null) return;

        double time = (level.getGameTime() + partialTick) / 20.0;
        float bob = (float) Math.sin(time * Math.PI * 0.25) * 0.04f;
        float rotY = (float) ((time * 10.0) % 360.0);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.XP.rotationDegrees(3f));

        ItemStack brainStack = new ItemStack(Registration.BRAIN.get());
        ItemStackRenderState brainRenderState = new ItemStackRenderState();
        Minecraft.getInstance()
                .getItemModelResolver()
                .updateForTopItem(brainRenderState, brainStack, ItemDisplayContext.FIXED, false, null, null, 0);
        brainRenderState.render(poseStack, bufferSource, packedLight, packedOverlay);

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
