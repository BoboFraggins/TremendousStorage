package net.bobofraggins.tremendousstorage.storage.chest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class ChestRenderer
        implements BlockEntityRenderer<ChestBlockEntity, ChestRenderer.State>,
                IBlockEntityRendererExtension<ChestBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/chest_body");
    static final StandaloneModelKey<BlockStateModel> LID_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/chest_lid");

    public static class State extends BlockEntityRenderState {
        public float lidAngle;
        public Direction facing = Direction.NORTH;
        public int color;
    }

    public ChestRenderer(BlockEntityRendererProvider.Context ctx) {}

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
        state.lidAngle = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        BlockState blockState = be.getBlockState();
        state.facing =
                blockState.hasProperty(ChestBlock.FACING) ? blockState.getValue(ChestBlock.FACING) : Direction.NORTH;
        state.color = be.getTier().getColor();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL_KEY);
        BlockStateModel lidModel = mc.getModelManager().getStandaloneModel(LID_MODEL_KEY);

        int color = state.color;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        int packedLight = state.lightCoords;
        int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float yRot = facingYRot(state.facing);
        RandomSource random = RandomSource.create();

        // Body
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        final var bodyPose = poseStack.last();
        final List<BlockModelPart> bodyParts = collectParts(bodyModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuadsWithShading(
                        consumer, bodyPose, bodyParts, null, r, g, b, packedLight, packedOverlay));
        poseStack.popPose();

        // Lid
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0, 9.0 / 16.0, 15.0 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.lidAngle * 90f));
        poseStack.translate(0.0, -9.0 / 16.0, -15.0 / 16.0);
        final var lidPose = poseStack.last();
        final List<BlockModelPart> lidParts = collectParts(lidModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) ->
                        renderQuadsWithShading(consumer, lidPose, lidParts, null, r, g, b, packedLight, packedOverlay));
        poseStack.popPose();
    }

    private static List<BlockModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    private static void renderQuadsWithShading(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BlockModelPart> parts,
            Level level,
            float r,
            float g,
            float b,
            int packedLight,
            int packedOverlay) {
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                    float qr, qg, qb;
                    if (quad.tintIndex() >= 0) {
                        qr = r * shade;
                        qg = g * shade;
                        qb = b * shade;
                    } else {
                        qr = shade;
                        qg = shade;
                        qb = shade;
                    }
                    consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
                }
            }
            for (var quad : part.getQuads(null)) {
                Direction dir = quad.direction();
                float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                float qr, qg, qb;
                if (quad.tintIndex() >= 0) {
                    qr = r * shade;
                    qg = g * shade;
                    qb = b * shade;
                } else {
                    qr = shade;
                    qg = shade;
                    qb = shade;
                }
                consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
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
