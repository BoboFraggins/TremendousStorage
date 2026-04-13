package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
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
 * Renders the body and animated split lids of the placed Picnic Basket.
 *
 * <p>The basket has two lids that open away from the center on the X axis:
 * <ul>
 *   <li>Left lid (right half of basket, x=8..13): rotates +Z around pivot [8/16, 8/16, 8/16]
 *   <li>Right lid (left half of basket, x=3..8): rotates −Z around pivot [8/16, 8/16, 8/16]
 * </ul>
 */
public class PicnicBasketRenderer
        implements BlockEntityRenderer<ChestBlockEntity>, IBlockEntityRendererExtension<ChestBlockEntity> {

    static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_body"));
    static final ModelResourceLocation LEFT_LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_left_lid"));
    static final ModelResourceLocation RIGHT_LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_right_lid"));

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public PicnicBasketRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            ChestBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel leftLidModel = mc.getModelManager().getModel(LEFT_LID_MODEL);
        BakedModel rightLidModel = mc.getModelManager().getModel(RIGHT_LID_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(PicnicBasketBlock.FACING);
        float yRot = facingYRot(facing);

        RandomSource random = RandomSource.create();

        // Body — facing rotation only.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderQuads(consumer, poseStack.last(), bodyModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);

        // Left lid — right half of basket (x=8..13) rotates +Z away from center.
        // Pivot: [8/16, 8/16, 8/16] as set in Blockbench.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(8.0 / 16.0, 8.0 / 16.0, 8.0 / 16.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(openFraction * 90f));
        poseStack.translate(-8.0 / 16.0, -8.0 / 16.0, -8.0 / 16.0);
        renderQuads(consumer, poseStack.last(), leftLidModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Right lid — left half of basket (x=3..8) rotates −Z away from center.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(8.0 / 16.0, 8.0 / 16.0, 8.0 / 16.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-openFraction * 90f));
        poseStack.translate(-8.0 / 16.0, -8.0 / 16.0, -8.0 / 16.0);
        renderQuads(consumer, poseStack.last(), rightLidModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();
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
                consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.solid())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ChestBlockEntity be) {
        return be.lidAngle > 0f || be.prevLidAngle > 0f;
    }

    @Override
    public AABB getRenderBoundingBox(ChestBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
