package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class PicnicBasketRenderer
        implements BlockEntityRenderer<ChestBlockEntity, PicnicBasketRenderer.State>,
                IBlockEntityRendererExtension<ChestBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/picnic_basket_body");
    static final StandaloneModelKey<BlockStateModel> LEFT_LID_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/picnic_basket_left_lid");
    static final StandaloneModelKey<BlockStateModel> RIGHT_LID_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/picnic_basket_right_lid");

    public static class State extends BlockEntityRenderState {
        public float facingYRot;
        public float openFraction;
    }

    public PicnicBasketRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    @Override
    public void extractRenderState(
            ChestBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(PicnicBasketBlock.FACING);
        state.facingYRot = facingYRot(facing);
        state.openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL);
        BlockStateModel leftLidModel = mc.getModelManager().getStandaloneModel(LEFT_LID_MODEL);
        BlockStateModel rightLidModel = mc.getModelManager().getStandaloneModel(RIGHT_LID_MODEL);
        int light = state.lightCoords;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float yRot = state.facingYRot;
        float openFraction = state.openFraction;
        RandomSource random = RandomSource.create();

        List<BlockStateModelPart> bodyParts = collectParts(bodyModel, random);
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        collector.submitCustomGeometry(
                poseStack,
                Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, bodyParts, light, overlay));
        poseStack.popPose();

        List<BlockStateModelPart> leftLidParts = collectParts(leftLidModel, random);
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(8.0 / 16.0, 8.0 / 16.0, 8.0 / 16.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(openFraction * 90f));
        poseStack.translate(-8.0 / 16.0, -8.0 / 16.0, -8.0 / 16.0);
        collector.submitCustomGeometry(
                poseStack,
                Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, leftLidParts, light, overlay));
        poseStack.popPose();

        List<BlockStateModelPart> rightLidParts = collectParts(rightLidModel, random);
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(8.0 / 16.0, 8.0 / 16.0, 8.0 / 16.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-openFraction * 90f));
        poseStack.translate(-8.0 / 16.0, -8.0 / 16.0, -8.0 / 16.0);
        collector.submitCustomGeometry(
                poseStack,
                Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, rightLidParts, light, overlay));
        poseStack.popPose();
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

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ChestBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
