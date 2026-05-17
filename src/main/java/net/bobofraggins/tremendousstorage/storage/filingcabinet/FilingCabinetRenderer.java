package net.bobofraggins.tremendousstorage.storage.filingcabinet;

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

public class FilingCabinetRenderer
        implements BlockEntityRenderer<FilingCabinetBlockEntity, FilingCabinetRenderer.State>,
                IBlockEntityRendererExtension<FilingCabinetBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/filing_cabinet_body");
    static final StandaloneModelKey<BlockStateModel> DRAWER_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/filing_cabinet_drawer");

    private static final float DRAWER_SLIDE = 8f / 16f;

    public static class State extends BlockEntityRenderState {
        public float drawerOffset;
        public float facingYRot;
    }

    public FilingCabinetRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            FilingCabinetBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.drawerOffset = Mth.lerp(partialTick, be.prevDrawerOffset, be.drawerOffset);
        Direction facing = be.getBlockState().getValue(FilingCabinetBlock.FACING);
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
        BlockStateModel drawerModel = mc.getModelManager().getStandaloneModel(DRAWER_MODEL);
        int packedLight = state.lightCoords;
        RandomSource random = RandomSource.create();

        poseStack.pushPose();
        applyFacingRotation(poseStack, state.facingYRot);
        List<BlockStateModelPart> bodyParts = collectParts(bodyModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, bodyParts, packedLight));
        poseStack.popPose();

        poseStack.pushPose();
        applyFacingRotation(poseStack, state.facingYRot);
        poseStack.translate(0.0, 0.0, -state.drawerOffset * DRAWER_SLIDE);
        List<BlockStateModelPart> drawerParts = collectParts(drawerModel, random);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, drawerParts, packedLight));
        poseStack.popPose();
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
                QuadInstance qi = new QuadInstance();
                qi.setColor(0xFFFFFFFF);
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }
}
