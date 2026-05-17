package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
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

public class ArmoryCabinetRenderer
        implements BlockEntityRenderer<ArmoryCabinetBlockEntity, ArmoryCabinetRenderer.State>,
                IBlockEntityRendererExtension<ArmoryCabinetBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY = key("block/armory_cabinet_body");
    static final StandaloneModelKey<BlockStateModel> DOOR = key("block/armory_cabinet_door");
    static final StandaloneModelKey<BlockStateModel> ARM_TOP = key("block/armory_cabinet_arm_top");
    static final StandaloneModelKey<BlockStateModel> ARM_BOTTOM = key("block/armory_cabinet_arm_bottom");
    static final StandaloneModelKey<BlockStateModel> WHEEL = key("block/armory_cabinet_wheel");

    private static StandaloneModelKey<BlockStateModel> key(String path) {
        return new StandaloneModelKey<>(() -> "tremendousstorage:" + path);
    }

    private static final float PHASE1_END = 0.6f;
    private static final float PHASE2_START = 0.6f;
    private static final float ARM_RETRACT_PX = 3f / 16f;
    private static final float DOOR_SWING_DEG = 90f;
    private static final float WHEEL_SPIN_DEG = 360f;
    private static final float HINGE_X = 15f / 16f;
    private static final float HINGE_Z = 8f / 16f;
    private static final float WHEEL_X = 9f / 16f;
    private static final float WHEEL_Y = 16f / 16f;
    private static final float WHEEL_Z = 1f / 16f;

    public static class State extends BlockEntityRenderState {
        public float facingYRot;
        public float openFraction;
    }

    public ArmoryCabinetRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ArmoryCabinetBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        Direction facing = be.getBlockState().getValue(ArmoryCabinetBlock.FACING);
        state.facingYRot = switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
        state.openFraction = Mth.lerp(partialTick, be.prevOpenFraction, be.openFraction);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        int packedLight = state.lightCoords;
        RandomSource random = RandomSource.create();
        float open = state.openFraction;
        float p1 = Math.min(open / PHASE1_END, 1f);
        float p2 = Math.max((open - PHASE2_START) / (1f - PHASE2_START), 0f);

        // Static body
        poseStack.pushPose();
        applyFacingRotation(poseStack, state.facingYRot);
        List<BlockStateModelPart> bodyParts = collectParts(mc.getModelManager().getStandaloneModel(BODY), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, bodyParts, packedLight));
        poseStack.popPose();

        // Door group
        poseStack.pushPose();
        applyFacingRotation(poseStack, state.facingYRot);
        poseStack.translate(HINGE_X, 0, HINGE_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-p2 * DOOR_SWING_DEG));
        poseStack.translate(-HINGE_X, 0, -HINGE_Z);

        List<BlockStateModelPart> doorParts = collectParts(mc.getModelManager().getStandaloneModel(DOOR), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, doorParts, packedLight));

        poseStack.pushPose();
        poseStack.translate(0, -ARM_RETRACT_PX * p1, 0);
        List<BlockStateModelPart> armTopParts =
                collectParts(mc.getModelManager().getStandaloneModel(ARM_TOP), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, armTopParts, packedLight));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, ARM_RETRACT_PX * p1, 0);
        List<BlockStateModelPart> armBotParts =
                collectParts(mc.getModelManager().getStandaloneModel(ARM_BOTTOM), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, armBotParts, packedLight));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(WHEEL_X, WHEEL_Y, WHEEL_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-WHEEL_SPIN_DEG * p1));
        poseStack.translate(-WHEEL_X, -WHEEL_Y, -WHEEL_Z);
        List<BlockStateModelPart> wheelParts = collectParts(mc.getModelManager().getStandaloneModel(WHEEL), random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, wheelParts, packedLight));
        poseStack.popPose();

        poseStack.popPose(); // end door group
    }

    private static void applyFacingRotation(PoseStack poseStack, float yDeg) {
        if (yDeg != 0f) {
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(yDeg));
            poseStack.translate(-0.5, -0.5, -0.5);
        }
    }

    @SuppressWarnings("deprecation")
    private static List<BlockStateModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static void renderModel(
            VertexConsumer consumer, PoseStack.Pose pose, List<BlockStateModelPart> parts, int packedLight) {
        int overlay = OverlayTexture.NO_OVERLAY;
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
                QuadInstance qi2 = new QuadInstance();
                qi2.setColor(0xFFFFFFFF);
                qi2.setLightCoords(packedLight);
                qi2.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi2);
            }
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
