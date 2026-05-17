package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;

public class RecyclingBinRenderer
        implements BlockEntityRenderer<RecyclingBinBlockEntity, RecyclingBinRenderer.State>,
                IBlockEntityRendererExtension<RecyclingBinBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/recycling_bin_body");
    static final StandaloneModelKey<BlockStateModel> LID_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/recycling_bin_lid");
    static final StandaloneModelKey<BlockStateModel> PEDAL_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/recycling_bin_pedal");

    private static final float LID_PIVOT_Y = 12f / 16f;
    private static final float LID_PIVOT_Z = 12f / 16f;

    private static final float PEDAL_PIVOT_X = 12f / 16f;
    private static final float PEDAL_PIVOT_Y = 1f / 16f;
    private static final float PEDAL_PIVOT_Z = 4f / 16f;

    private static final float FLUID_X0 = 1f / 16f;
    private static final float FLUID_X1 = 7f / 16f;
    private static final float FLUID_Z0 = 5f / 16f;
    private static final float FLUID_Z1 = 11f / 16f;
    static final float FLUID_FLOOR = 1f / 16f;
    static final float FLUID_CEIL = 9f / 16f;
    static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    public static class State extends BlockEntityRenderState {
        public float facingYRot;
        public float openFraction;
        public float fillFraction;
    }

    public RecyclingBinRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            RecyclingBinBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(RecyclingBinBlock.FACING);
        state.facingYRot = switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
        state.openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        state.fillFraction = (float) be.getVibesAmount() / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL_KEY);
        BlockStateModel lidModel = mc.getModelManager().getStandaloneModel(LID_MODEL_KEY);
        BlockStateModel pedalModel = mc.getModelManager().getStandaloneModel(PEDAL_MODEL_KEY);
        int light = state.lightCoords;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float yRot = state.facingYRot;
        float openFraction = state.openFraction;
        RandomSource random = RandomSource.create();

        // Body
        List<BlockStateModelPart> bodyParts = collectParts(bodyModel, random);
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        collector.submitCustomGeometry(
                poseStack, Sheets.cutoutBlockSheet(), (pose, vc) -> renderModel(vc, pose, bodyParts, light, overlay));
        poseStack.popPose();

        // Lid
        List<BlockStateModelPart> lidParts = collectParts(lidModel, random);
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(0.0, LID_PIVOT_Y, LID_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -LID_PIVOT_Y, -LID_PIVOT_Z);
        collector.submitCustomGeometry(
                poseStack, Sheets.cutoutBlockSheet(), (pose, vc) -> renderModel(vc, pose, lidParts, light, overlay));
        poseStack.popPose();

        // Pedal
        List<BlockStateModelPart> pedalParts = collectParts(pedalModel, random);
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(PEDAL_PIVOT_X, PEDAL_PIVOT_Y, PEDAL_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * -22.5f));
        poseStack.translate(-PEDAL_PIVOT_X, -PEDAL_PIVOT_Y, -PEDAL_PIVOT_Z);
        collector.submitCustomGeometry(
                poseStack, Sheets.cutoutBlockSheet(), (pose, vc) -> renderModel(vc, pose, pedalParts, light, overlay));
        poseStack.popPose();

        // Fluid fill
        float fill = Math.max(0.01f, state.fillFraction);
        float fillTop = FLUID_FLOOR + fill * FLUID_H;

        net.minecraft.client.renderer.block.FluidModel fluidModel_ = net.minecraft.client.Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(net.bobofraggins.tremendousstorage.shared.register.Registration.POSITIVE_VIBES_SOURCE
                        .get()
                        .defaultFluidState());
        TextureAtlasSprite sprite = fluidModel_.stillMaterial().sprite();
        int tint = fluidModel_.fluidTintSource() != null
                ? fluidModel_
                        .fluidTintSource()
                        .colorAsStack(new net.neoforged.neoforge.fluids.FluidStack(
                                Registration.POSITIVE_VIBES_SOURCE.get(), 1))
                : 0xFFFFFFFF;
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 77;
        int fluidLight = Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? 0xF000F0 : light;
        float uL = sprite.getU0(), uR = sprite.getU1();
        float vT = sprite.getV0(), vB = Mth.lerp(fill, sprite.getV0(), sprite.getV1());

        final int ffrFinal = fr, ffgFinal = fg, ffbFinal = fb, ffaFinal = fa, fluidLightFinal = fluidLight;
        final int overlayFinal = overlay;
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        collector.submitCustomGeometry(
                poseStack,
                Sheets.translucentItemSheet(),
                (pose, vc) -> renderFluidGeometry(
                        vc,
                        pose.pose(),
                        ffrFinal,
                        ffgFinal,
                        ffbFinal,
                        ffaFinal,
                        fluidLightFinal,
                        overlayFinal,
                        uL,
                        uR,
                        vT,
                        vB,
                        fillTop));
        poseStack.popPose();
    }

    static void renderFluidGeometry(
            VertexConsumer vc,
            Matrix4f mat,
            int fr,
            int fg,
            int fb,
            int fa,
            int light,
            int overlay,
            float uL,
            float uR,
            float vT,
            float vB,
            float fillTop) {
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                light,
                overlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X0,
                fillTop,
                FLUID_Z0,
                FLUID_X1,
                fillTop,
                FLUID_Z0,
                0,
                0,
                -1);
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                light,
                overlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X1,
                fillTop,
                FLUID_Z1,
                FLUID_X0,
                fillTop,
                FLUID_Z1,
                0,
                0,
                1);
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                light,
                overlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X0,
                fillTop,
                FLUID_Z1,
                FLUID_X0,
                fillTop,
                FLUID_Z0,
                -1,
                0,
                0);
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                light,
                overlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X1,
                fillTop,
                FLUID_Z0,
                FLUID_X1,
                fillTop,
                FLUID_Z1,
                1,
                0,
                0);
        quad(
                vc, mat, fr, fg, fb, fa, light, overlay, uL, vB, uR, vT, FLUID_X0, fillTop, FLUID_Z0, FLUID_X0, fillTop,
                FLUID_Z1, FLUID_X1, fillTop, FLUID_Z1, FLUID_X1, fillTop, FLUID_Z0, 0, 1, 0);
    }

    private static void applyFacingRotation(PoseStack poseStack, float yRot) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
    }

    @SuppressWarnings("deprecation")
    private static List<BlockStateModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static void renderModel(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BlockStateModelPart> parts,
            int packedLight,
            int overlay) {
        for (BlockStateModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    QuadInstance qi = new QuadInstance();
                    qi.setColor(0xFFFFFFFF);
                    qi.setLightCoords(packedLight);
                    qi.setOverlayCoords(overlay);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
            for (var quad : part.getQuads(null)) {
                QuadInstance qi = new QuadInstance();
                qi.setColor(0xFFFFFFFF);
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }

    @SuppressWarnings("java:S107")
    private static void quad(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay,
            float u0,
            float v0,
            float u1,
            float v1,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz) {
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, a)
                .setUv(u1, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, a)
                .setUv(u0, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(RecyclingBinBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
