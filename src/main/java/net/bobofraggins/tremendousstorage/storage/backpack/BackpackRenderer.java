package net.bobofraggins.tremendousstorage.storage.backpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BackpackRenderer
        implements BlockEntityRenderer<ChestBlockEntity, BackpackRenderer.State>,
                IBlockEntityRendererExtension<ChestBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/backpack_body");
    static final StandaloneModelKey<BlockStateModel> FLAP_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/backpack_flap");

    public static class State extends BlockEntityRenderState {
        public float flapAngle;
        public float facingYRot;
    }

    public BackpackRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ChestBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.flapAngle = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        Direction facing = be.getBlockState().getValue(BackpackBlock.FACING);
        state.facingYRot = switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL);
        BlockStateModel flapModel = mc.getModelManager().getStandaloneModel(FLAP_MODEL);
        int packedLight = state.lightCoords;
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facingYRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        List<BlockModelPart> bodyParts = collectParts(bodyModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuads(consumer, pose, bodyParts, packedLight));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facingYRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0, 12.0 / 16.0, 12.251 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.flapAngle * 90f));
        poseStack.translate(0.0, -12.0 / 16.0, -12.251 / 16.0);
        List<BlockModelPart> flapParts = collectParts(flapModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuads(consumer, pose, flapParts, packedLight));
        poseStack.popPose();
    }

    private static List<BlockModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static void renderQuads(
            VertexConsumer consumer, PoseStack.Pose pose, List<BlockModelPart> parts, int packedLight) {
        int overlay = OverlayTexture.NO_OVERLAY;
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1.0f, packedLight, overlay);
                }
            }
            for (var quad : part.getQuads(null)) {
                consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1.0f, packedLight, overlay);
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
