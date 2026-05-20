package net.bobofraggins.tremendousstorage.storage.wirelesshub;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class WirelessHubRenderer
        implements BlockEntityRenderer<WirelessHubBlockEntity, WirelessHubRenderer.State>,
                IBlockEntityRendererExtension<WirelessHubBlockEntity> {

    public static final StandaloneModelKey<BlockStateModel> BASE_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/wireless_hub_base");
    public static final StandaloneModelKey<BlockStateModel> DISH_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/wireless_hub_dish");
    public static final StandaloneModelKey<BlockStateModel> BASE_HIGHLIGHT_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/wireless_hub_highlight");
    public static final StandaloneModelKey<BlockStateModel> DISH_HIGHLIGHT_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/wireless_hub_dish_highlight");

    private static final float PIVOT = 0.5f;

    public static class State extends BlockEntityRenderState {
        public float facingYRot;
        public float r, g, b;
        public boolean connected;
        public float dishAngle;
    }

    public WirelessHubRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            WirelessHubBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        Direction facing = be.getBlockState().getValue(WirelessHubBlock.FACING);
        state.facingYRot = switch (facing) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
        int color = be.getTier().getColor();
        state.r = ((color >> 16) & 0xFF) / 255f;
        state.g = ((color >> 8) & 0xFF) / 255f;
        state.b = (color & 0xFF) / 255f;
        state.connected = be.isConnected();
        state.dishAngle = be.getDishAngle(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel baseModel = mc.getModelManager().getStandaloneModel(BASE_MODEL);
        BlockStateModel dishModel = mc.getModelManager().getStandaloneModel(DISH_MODEL);
        BlockStateModel baseHighlightModel = mc.getModelManager().getStandaloneModel(BASE_HIGHLIGHT_MODEL);
        BlockStateModel dishHighlightModel = mc.getModelManager().getStandaloneModel(DISH_HIGHLIGHT_MODEL);
        int packedLight = state.lightCoords;
        float r = state.r, g = state.g, b = state.b;
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        applyFacingRotation(poseStack, state.facingYRot);
        List<BlockStateModelPart> baseParts = collectParts(baseModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, baseParts, r, g, b, packedLight));
        List<BlockStateModelPart> baseHighlightParts = collectParts(baseHighlightModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderHighlight(consumer, pose, baseHighlightParts, r, g, b, packedLight));
        poseStack.popPose();

        poseStack.pushPose();
        applyFacingRotation(poseStack, state.facingYRot);
        if (state.connected) {
            poseStack.translate(PIVOT, PIVOT, PIVOT);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.dishAngle));
            poseStack.translate(-PIVOT, -PIVOT, -PIVOT);
        }
        List<BlockStateModelPart> dishParts = collectParts(dishModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, dishParts, r, g, b, packedLight));
        List<BlockStateModelPart> dishHighlightParts = collectParts(dishHighlightModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderHighlight(consumer, pose, dishHighlightParts, r, g, b, packedLight));
        poseStack.popPose();
    }

    private static void applyFacingRotation(PoseStack poseStack, float yDeg) {
        if (yDeg != 0f) {
            poseStack.translate(PIVOT, PIVOT, PIVOT);
            poseStack.mulPose(Axis.YP.rotationDegrees(yDeg));
            poseStack.translate(-PIVOT, -PIVOT, -PIVOT);
        }
    }

    @SuppressWarnings("deprecation")
    private static List<BlockStateModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static void renderHighlight(
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
                    float shade = quad.materialInfo().shade() ? directionShade(dir) : 1f;
                    QuadInstance qi = new QuadInstance();
                    qi.setColor((0xFF << 24)
                            | ((int) (r * shade * 255) << 16)
                            | ((int) (g * shade * 255) << 8)
                            | (int) (b * shade * 255));
                    qi.setLightCoords(packedLight);
                    qi.setOverlayCoords(overlay);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
            for (var quad : part.getQuads(null)) {
                float shade = quad.materialInfo().shade() ? directionShade(quad.direction()) : 1f;
                QuadInstance qi = new QuadInstance();
                qi.setColor((0xFF << 24)
                        | ((int) (r * shade * 255) << 16)
                        | ((int) (g * shade * 255) << 8)
                        | (int) (b * shade * 255));
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }

    private static float directionShade(Direction dir) {
        return switch (dir) {
            case DOWN -> 0.5f;
            case UP -> 1.0f;
            case NORTH, SOUTH -> 0.8f;
            case EAST, WEST -> 0.6f;
        };
    }

    private static void renderModel(
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
                    float shade = quad.materialInfo().shade() ? directionShade(dir) : 1f;
                    float qr, qg, qb;
                    if (quad.materialInfo().tintIndex() >= 0) {
                        qr = r * shade;
                        qg = g * shade;
                        qb = b * shade;
                    } else {
                        qr = qg = qb = shade;
                    }
                    QuadInstance qi = new QuadInstance();
                    qi.setColor((0xFF << 24) | ((int) (qr * 255) << 16) | ((int) (qg * 255) << 8) | (int) (qb * 255));
                    qi.setLightCoords(packedLight);
                    qi.setOverlayCoords(overlay);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
            for (var quad : part.getQuads(null)) {
                float shade = quad.materialInfo().shade() ? directionShade(quad.direction()) : 1f;
                float qr, qg, qb;
                if (quad.materialInfo().tintIndex() >= 0) {
                    qr = r * shade;
                    qg = g * shade;
                    qb = b * shade;
                } else {
                    qr = qg = qb = shade;
                }
                QuadInstance qi = new QuadInstance();
                qi.setColor((0xFF << 24) | ((int) (qr * 255) << 16) | ((int) (qg * 255) << 8) | (int) (qb * 255));
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }
}
