package net.bobofraggins.tremendousstorage.power.stirlingengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class StirlingEngineRenderer
        implements BlockEntityRenderer<StirlingEngineBlockEntity, StirlingEngineRenderer.State>,
                IBlockEntityRendererExtension<StirlingEngineBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_body");
    static final StandaloneModelKey<BlockStateModel> FLYWHEEL_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_flywheel");
    static final StandaloneModelKey<BlockStateModel> PISTON_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_piston");
    static final StandaloneModelKey<BlockStateModel> BRIDGE_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_bridge");

    private static final float FW_X = 1.5f / 16f;
    private static final float FW_Y = 7.0f / 16f;
    private static final float FW_Z = 8.0f / 16f;
    private static final float MAX_PISTON_RETRACT = 4.0f / 16f;
    private static final float FA_REL_Y = 0.0f;
    private static final float FA_REL_Z = 3.0f / 16f;
    private static final float ARM_CX = 4.5f / 16f;
    private static final float ARM_CY = 7.0f / 16f;
    private static final float ARM_Z0 = 0.5f / 16f;
    private static final float ARM_REST_LEN = 10.5f / 16f;

    public static class State extends BlockEntityRenderState {
        public float animT;
        public float tr, tg, tb;
    }

    public StirlingEngineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            StirlingEngineBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.animT = be.isHeated() ? (be.animationTicks + partialTick) / StirlingEngineBlockEntity.CYCLE_TICKS : 0f;
        int tierColor = be.getTier().getColor();
        state.tr = ((tierColor >> 16) & 0xFF) / 255f;
        state.tg = ((tierColor >> 8) & 0xFF) / 255f;
        state.tb = (tierColor & 0xFF) / 255f;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        int packedLight = state.lightCoords;
        RandomSource random = RandomSource.create();

        float t = state.animT;
        float theta = Mth.TWO_PI * t;
        float cosT = Mth.cos(theta);
        float sinT = Mth.sin(theta);
        float pistonZ = MAX_PISTON_RETRACT * (1f + cosT) / 2f;
        float tr = state.tr, tg = state.tg, tb = state.tb;

        // Body
        poseStack.pushPose();
        List<BlockStateModelPart> bodyParts = collectParts(mc.getModelManager().getStandaloneModel(BODY_MODEL), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuads(consumer, pose, bodyParts, 1f, 1f, 1f, packedLight));
        poseStack.popPose();

        // Flywheel
        poseStack.pushPose();
        poseStack.translate(FW_X, FW_Y, FW_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(360f * t));
        poseStack.translate(-FW_X, -FW_Y, -FW_Z);
        List<BlockStateModelPart> fwParts =
                collectParts(mc.getModelManager().getStandaloneModel(FLYWHEEL_MODEL), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuads(consumer, pose, fwParts, tr, tg, tb, packedLight));
        poseStack.popPose();

        // Piston
        poseStack.pushPose();
        poseStack.translate(0f, 0f, pistonZ);
        List<BlockStateModelPart> pistonParts =
                collectParts(mc.getModelManager().getStandaloneModel(PISTON_MODEL), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuads(consumer, pose, pistonParts, 1f, 1f, 1f, packedLight));
        poseStack.popPose();

        // Bridge connector
        float paX = ARM_CX, paY = ARM_CY, paZ = ARM_Z0 + pistonZ;
        float faY = FW_Y + FA_REL_Y * cosT - FA_REL_Z * sinT;
        float faZ = FW_Z + FA_REL_Y * sinT + FA_REL_Z * cosT;
        float dy = faY - paY, dz = faZ - paZ;
        float yzDist = Mth.sqrt(dy * dy + dz * dz);
        if (yzDist > 1e-4f) {
            poseStack.pushPose();
            poseStack.translate(paX, paY, paZ);
            poseStack.mulPose(Axis.XP.rotation((float) Math.atan2(-dy, dz)));
            poseStack.scale(1f, 1f, yzDist / ARM_REST_LEN);
            poseStack.translate(-ARM_CX, -ARM_CY, -ARM_Z0);
            List<BlockStateModelPart> bridgeParts =
                    collectParts(mc.getModelManager().getStandaloneModel(BRIDGE_MODEL), random);
            collector.submitCustomGeometry(
                    poseStack,
                    net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                    (pose, consumer) -> renderQuads(consumer, pose, bridgeParts, 1f, 1f, 1f, packedLight));
            poseStack.popPose();
        }
    }

    private static List<BlockStateModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static void renderQuads(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BlockStateModelPart> parts,
            float r,
            float g,
            float b,
            int packedLight) {
        int overlay = OverlayTexture.NO_OVERLAY;
        for (BlockStateModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    int color = quad.materialInfo().tintIndex() >= 0
                            ? (0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255))
                            : 0xFFFFFFFF;
                    QuadInstance qi = new QuadInstance();
                    qi.setColor(color);
                    qi.setLightCoords(packedLight);
                    qi.setOverlayCoords(overlay);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
            for (var quad : part.getQuads(null)) {
                int color = quad.materialInfo().tintIndex() >= 0
                        ? (0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255))
                        : 0xFFFFFFFF;
                QuadInstance qi = new QuadInstance();
                qi.setColor(color);
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }
}
