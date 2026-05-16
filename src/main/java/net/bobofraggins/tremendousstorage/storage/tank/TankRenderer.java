package net.bobofraggins.tremendousstorage.storage.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.tremendousstorage.shared.tank.AbstractTankRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class TankRenderer extends AbstractTankRenderer<TankBlockEntity> {

    public static final float TANK_FLUID_FLOOR = 1f / 16f;
    public static final float TANK_FLUID_CEIL = 15f / 16f;
    public static final float TANK_FLUID_H = TANK_FLUID_CEIL - TANK_FLUID_FLOOR;
    private static final float TANK_X0 = 1f / 16f;
    private static final float TANK_X1 = 15f / 16f;
    private static final float TANK_Z0 = 1f / 16f;
    private static final float TANK_Z1 = 15f / 16f;

    public TankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected void renderFill(
            State state, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        // Fluid rendering handled in submit() override using TankState fields
    }

    @Override
    public State createRenderState() {
        return new TankState();
    }

    public static class TankState extends State {
        public boolean isLocked;
        public int fr, fg, fb, fa;
        public float fillFrac;
        public float fillTop;
        public float uL, uR, vT, vB;
        public int fluidLight;
        public boolean hasFill;
    }

    @Override
    public void extractRenderState(
            TankBlockEntity be,
            State stateBase,
            float partialTick,
            net.minecraft.world.phys.Vec3 camera,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(be, stateBase, partialTick, camera, breakProgress);
        if (stateBase instanceof TankState state) {
            state.isLocked = be.isLocked();
            state.hasFill = false;
            if (state.isLocked) {
                FluidStack fluid = be.getStoredFluid();
                if (!fluid.isEmpty()) {
                    state.fillFrac = Math.max(0.01f, (float) be.getAmount() / be.getCapacity());
                    state.fillTop = TANK_FLUID_FLOOR + state.fillFrac * TANK_FLUID_H;
                    net.minecraft.client.renderer.block.FluidModel fluidModel_ =
                            net.minecraft.client.Minecraft.getInstance()
                                    .getModelManager()
                                    .getFluidStateModelSet()
                                    .get(fluid.getFluid().defaultFluidState());
                    var fluidSprite = fluidModel_.stillMaterial().sprite();
                    int tint = fluidModel_.fluidTintSource() != null
                            ? fluidModel_.fluidTintSource().colorAsStack(fluid)
                            : 0xFFFFFFFF;
                    state.fr = (tint >> 16) & 0xFF;
                    state.fg = (tint >> 8) & 0xFF;
                    state.fb = tint & 0xFF;
                    state.fa = (tint >> 24) & 0xFF;
                    if (state.fa == 0) state.fa = 77;
                    state.fluidLight = fluid.getFluidType().getLightLevel() > 0 ? 0xF000F0 : stateBase.lightCoords;
                    state.uL = fluidSprite.getU0();
                    state.uR = fluidSprite.getU1();
                    state.vT = fluidSprite.getV0();
                    state.vB = Mth.lerp(state.fillFrac, fluidSprite.getV0(), fluidSprite.getV1());
                    state.hasFill = true;
                }
            }
        }
    }

    @Override
    public void submit(
            State stateBase, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (stateBase instanceof TankState state && state.hasFill) {
            int overlayFinal = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            float fillTop = state.fillTop;
            int fr = state.fr, fg = state.fg, fb = state.fb, fa = state.fa;
            int fluidLight = state.fluidLight;
            float uL = state.uL, uR = state.uR, vT = state.vT, vB = state.vB;
            poseStack.pushPose();
            collector.submitCustomGeometry(
                    poseStack,
                    Sheets.translucentItemSheet(),
                    (pose, vc) -> renderCubeFill(
                            vc, pose.pose(), fr, fg, fb, fa, fluidLight, overlayFinal, uL, vT, uR, vB, fillTop));
            poseStack.popPose();
        }
        // Render stubs via super
        if (stateBase.hasStub) {
            super.submit(stateBase, poseStack, collector, cameraState);
        }
    }

    public static void renderCubeFill(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay,
            float uL,
            float vT,
            float uR,
            float vB,
            float fillTop) {
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uL,
                vT,
                uR,
                vB,
                TANK_X1,
                TANK_FLUID_FLOOR,
                TANK_Z0,
                TANK_X0,
                TANK_FLUID_FLOOR,
                TANK_Z0,
                TANK_X0,
                fillTop,
                TANK_Z0,
                TANK_X1,
                fillTop,
                TANK_Z0,
                0,
                0,
                -1);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uL,
                vT,
                uR,
                vB,
                TANK_X0,
                TANK_FLUID_FLOOR,
                TANK_Z1,
                TANK_X1,
                TANK_FLUID_FLOOR,
                TANK_Z1,
                TANK_X1,
                fillTop,
                TANK_Z1,
                TANK_X0,
                fillTop,
                TANK_Z1,
                0,
                0,
                1);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uL,
                vT,
                uR,
                vB,
                TANK_X0,
                TANK_FLUID_FLOOR,
                TANK_Z0,
                TANK_X0,
                TANK_FLUID_FLOOR,
                TANK_Z1,
                TANK_X0,
                fillTop,
                TANK_Z1,
                TANK_X0,
                fillTop,
                TANK_Z0,
                -1,
                0,
                0);
        quadFluid(
                vc,
                mat,
                r,
                g,
                b,
                a,
                light,
                overlay,
                uL,
                vT,
                uR,
                vB,
                TANK_X1,
                TANK_FLUID_FLOOR,
                TANK_Z1,
                TANK_X1,
                TANK_FLUID_FLOOR,
                TANK_Z0,
                TANK_X1,
                fillTop,
                TANK_Z0,
                TANK_X1,
                fillTop,
                TANK_Z1,
                1,
                0,
                0);
        quadFluid(
                vc, mat, r, g, b, a, light, overlay, uL, vT, uR, vB, TANK_X0, fillTop, TANK_Z0, TANK_X0, fillTop,
                TANK_Z1, TANK_X1, fillTop, TANK_Z1, TANK_X1, fillTop, TANK_Z0, 0, 1, 0);
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

        float topUa = Mth.lerp(3.5f / 12f, uT0, uT1);
        float topUb = Mth.lerp(8.5f / 12f, uT0, uT1);
        float topVh = Mth.lerp(3.5f / 12f, vT0, vT1);
        float topVg = Mth.lerp(8.5f / 12f, vT0, vT1);
        quadFluidV(
                vc, mat, r, g, b, a, light, overlay, HX, fillTop, HZ, uT0, topVh, CX, fillTop, CZ, uT1, topVh, BX,
                fillTop, BZ, topUb, vT0, AX, fillTop, AZ, topUa, vT0, 0f, 1f, 0f);
        quadFluid(
                vc, mat, r, g, b, a, light, overlay, uT0, topVh, uT1, topVg, GX, fillTop, GZ, DX, fillTop, DZ, CX,
                fillTop, CZ, HX, fillTop, HZ, 0f, 1f, 0f);
        quadFluidV(
                vc, mat, r, g, b, a, light, overlay, FX, fillTop, FZ, topUa, vT1, EX, fillTop, EZ, topUb, vT1, DX,
                fillTop, DZ, uT1, topVg, GX, fillTop, GZ, uT0, topVg, 0f, 1f, 0f);
    }
}
