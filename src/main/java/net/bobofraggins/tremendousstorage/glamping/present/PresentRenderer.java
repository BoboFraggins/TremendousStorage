package net.bobofraggins.tremendousstorage.glamping.present;

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
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class PresentRenderer
        implements BlockEntityRenderer<PresentBlockEntity, PresentRenderer.State>,
                IBlockEntityRendererExtension<PresentBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/present");

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    public PresentRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            PresentBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        BlockState blockState = be.getBlockState();
        state.facing = blockState.hasProperty(PresentBlock.FACING)
                ? blockState.getValue(PresentBlock.FACING)
                : Direction.NORTH;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getStandaloneModel(MODEL_KEY);
        int packedLight = state.lightCoords;
        int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float yRot = facingYRot(state.facing);
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        final List<BlockStateModelPart> parts = collectParts(model, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderQuadsWithShading(consumer, pose, parts, packedLight, packedOverlay));
        poseStack.popPose();
    }

    @SuppressWarnings("deprecation")
    private static List<BlockStateModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
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

    private static float directionShade(Direction dir) {
        return switch (dir) {
            case DOWN -> 0.5f;
            case UP -> 1.0f;
            case NORTH, SOUTH -> 0.8f;
            case EAST, WEST -> 0.6f;
        };
    }

    private static void renderQuadsWithShading(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BlockStateModelPart> parts,
            int packedLight,
            int packedOverlay) {
        for (BlockStateModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    float shade = quad.materialInfo().shade() ? directionShade(dir) : 1f;
                    int c = (int) (shade * 255);
                    QuadInstance qi = new QuadInstance();
                    qi.setColor((0xFF << 24) | (c << 16) | (c << 8) | c);
                    qi.setLightCoords(packedLight);
                    qi.setOverlayCoords(packedOverlay);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
            for (var quad : part.getQuads(null)) {
                float shade = quad.materialInfo().shade() ? directionShade(quad.direction()) : 1f;
                int c = (int) (shade * 255);
                QuadInstance qi = new QuadInstance();
                qi.setColor((0xFF << 24) | (c << 16) | (c << 8) | c);
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(packedOverlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(PresentBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
