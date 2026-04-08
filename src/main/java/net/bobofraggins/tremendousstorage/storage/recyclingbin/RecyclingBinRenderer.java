package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders the Recycling Bin in three parts:
 * <ul>
 *   <li>Body — static, rendered with facing Y-rotation only.
 *   <li>Lid — animates open (rotates up by up to 90°) around the hinge at
 *       the back-top edge of the body (y=13/16, z=13/16).
 *   <li>Pedal — animates in sync with the lid (rotates down by up to 22.5°)
 *       around the top-back edge of the pedal (x=11/16, y=2/16, z=3/16).
 * </ul>
 */
public class RecyclingBinRenderer
        implements BlockEntityRenderer<RecyclingBinBlockEntity>,
                IBlockEntityRendererExtension<RecyclingBinBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_body"));
    private static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_lid"));
    private static final ModelResourceLocation PEDAL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_pedal"));

    // Lid hinge: bottom-back edge of the lid panel (in block space, 0-1 units).
    // The lid cubes span y=13-14 and z=3-13; the hinge is at y=13/16, z=13/16.
    private static final float LID_PIVOT_Y = 13f / 16f;
    private static final float LID_PIVOT_Z = 13f / 16f;

    // Pedal pivot: origin=[11, 2, 3] in Blockbench 0-16 units → 0-1 block space.
    private static final float PEDAL_PIVOT_X = 11f / 16f;
    private static final float PEDAL_PIVOT_Y = 2f / 16f;
    private static final float PEDAL_PIVOT_Z = 3f / 16f;

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public RecyclingBinRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            RecyclingBinBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel lidModel = mc.getModelManager().getModel(LID_MODEL);
        BakedModel pedalModel = mc.getModelManager().getModel(PEDAL_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(RecyclingBinBlock.FACING);
        float yRot = facingYRot(facing);

        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        RandomSource random = RandomSource.create();

        // Body — facing rotation only.
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        renderQuads(consumer, poseStack.last(), bodyModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Lid — facing rotation + pivot open animation.
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(0.0, LID_PIVOT_Y, LID_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -LID_PIVOT_Y, -LID_PIVOT_Z);
        renderQuads(consumer, poseStack.last(), lidModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Pedal — facing rotation + pivot press animation (rotates down).
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(PEDAL_PIVOT_X, PEDAL_PIVOT_Y, PEDAL_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * -22.5f));
        poseStack.translate(-PEDAL_PIVOT_X, -PEDAL_PIVOT_Y, -PEDAL_PIVOT_Z);
        renderQuads(consumer, poseStack.last(), pedalModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();
    }

    private static void applyFacingRotation(PoseStack poseStack, float yRot) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
    }

    private static void renderQuads(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedModel model,
            BlockState blockState,
            Level level,
            int packedLight,
            int packedOverlay,
            RandomSource random) {
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.solid())) {
                float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.solid())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            consumer.putBulkData(pose, quad, shade, shade, shade, 1f, packedLight, packedOverlay);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(RecyclingBinBlockEntity be) {
        return be.lidAngle > 0f || be.prevLidAngle > 0f;
    }

    @Override
    public AABB getRenderBoundingBox(RecyclingBinBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
