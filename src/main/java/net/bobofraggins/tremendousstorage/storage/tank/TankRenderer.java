package net.bobofraggins.tremendousstorage.storage.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.tremendousstorage.shared.tank.AbstractTankRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the Tank's dynamic content: fluid fill level and tube-connector stubs.
 *
 * <p>The static shell is rendered by the block model ({@code models/block/tank.json}).
 * This BESR handles the translucent octagonal-prism fill and lazurite connector stubs.
 */
public class TankRenderer extends AbstractTankRenderer<TankBlockEntity> {

    public TankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            TankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();
        renderFill(be, poseStack.last().pose(), bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    protected void renderFill(
            TankBlockEntity be,
            Matrix4f mat,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        if (!be.isLocked()) return;

        FluidStack fluid = be.getStoredFluid();

        float fillFrac = Math.max(0.01f, (float) be.getAmount() / be.getCapacity());
        float fillTop = FLUID_FLOOR + fillFrac * FLUID_H;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        var fluidSprite = sprite(ext.getStillTexture(fluid));

        int tint = ext.getTintColor(fluid);
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 77;

        int fluidLight = fluid.getFluidType().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        float uLeft = fluidSprite.getU0();
        float uRight = fluidSprite.getU1();
        float vTop = fluidSprite.getV0();
        float vBottom = Mth.lerp(fillFrac, fluidSprite.getV0(), fluidSprite.getV1());

        float uT0 = fluidSprite.getU0();
        float uT1 = fluidSprite.getU1();
        float vT0 = fluidSprite.getV0();
        float vT1 = fluidSprite.getV1();

        renderOctagonalPrism(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                uT0,
                uT1,
                vT0,
                vT1,
                fillTop);
    }

    public static void renderOctagonalPrism(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay,
            float uLeft,
            float vTop,
            float uRight,
            float vBottom,
            float uT0,
            float uT1,
            float vT0,
            float vT1,
            float fillTop) {

        // 8 side faces — CCW winding when viewed from outside
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                BX,
                FLUID_FLOOR,
                BZ,
                AX,
                FLUID_FLOOR,
                AZ,
                AX,
                fillTop,
                AZ,
                BX,
                fillTop,
                BZ,
                0f,
                0f,
                -1f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                CX,
                FLUID_FLOOR,
                CZ,
                BX,
                FLUID_FLOOR,
                BZ,
                BX,
                fillTop,
                BZ,
                CX,
                fillTop,
                CZ,
                0.7071f,
                0f,
                -0.7071f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                DX,
                FLUID_FLOOR,
                DZ,
                CX,
                FLUID_FLOOR,
                CZ,
                CX,
                fillTop,
                CZ,
                DX,
                fillTop,
                DZ,
                1f,
                0f,
                0f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                EX,
                FLUID_FLOOR,
                EZ,
                DX,
                FLUID_FLOOR,
                DZ,
                DX,
                fillTop,
                DZ,
                EX,
                fillTop,
                EZ,
                0.7071f,
                0f,
                0.7071f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                FX,
                FLUID_FLOOR,
                FZ,
                EX,
                FLUID_FLOOR,
                EZ,
                EX,
                fillTop,
                EZ,
                FX,
                fillTop,
                FZ,
                0f,
                0f,
                1f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                GX,
                FLUID_FLOOR,
                GZ,
                FX,
                FLUID_FLOOR,
                FZ,
                FX,
                fillTop,
                FZ,
                GX,
                fillTop,
                GZ,
                -0.7071f,
                0f,
                0.7071f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                HX,
                FLUID_FLOOR,
                HZ,
                GX,
                FLUID_FLOOR,
                GZ,
                GX,
                fillTop,
                GZ,
                HX,
                fillTop,
                HZ,
                -1f,
                0f,
                0f);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                AX,
                FLUID_FLOOR,
                AZ,
                HX,
                FLUID_FLOOR,
                HZ,
                HX,
                fillTop,
                HZ,
                AX,
                fillTop,
                AZ,
                -0.7071f,
                0f,
                -0.7071f);

        // Top face: octagonal prism cap decomposed into 3 quads (CCW from above)
        float topUa = Mth.lerp(3.5f / 12f, uT0, uT1);
        float topUb = Mth.lerp(8.5f / 12f, uT0, uT1);
        float topVh = Mth.lerp(3.5f / 12f, vT0, vT1);
        float topVg = Mth.lerp(8.5f / 12f, vT0, vT1);

        // North trapezoid: H→C→B→A
        quadFluidV(
                vc, mat, r, g, b, a, light, overlay, HX, fillTop, HZ, uT0, topVh, CX, fillTop, CZ, uT1, topVh, BX,
                fillTop, BZ, topUb, vT0, AX, fillTop, AZ, topUa, vT0, 0f, 1f, 0f);

        // Middle rectangle: G→D→C→H
        quadFluid(
                vc, mat, r, g, b, a, light, overlay, uT0, topVh, uT1, topVg, GX, fillTop, GZ, DX, fillTop, DZ, CX,
                fillTop, CZ, HX, fillTop, HZ, 0f, 1f, 0f);

        // South trapezoid: F→E→D→G
        quadFluidV(
                vc, mat, r, g, b, a, light, overlay, FX, fillTop, FZ, topUa, vT1, EX, fillTop, EZ, topUb, vT1, DX,
                fillTop, DZ, uT1, topVg, GX, fillTop, GZ, uT0, topVg, 0f, 1f, 0f);
    }
}
