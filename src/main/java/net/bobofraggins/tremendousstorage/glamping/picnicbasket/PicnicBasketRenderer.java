package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

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

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/picnic_basket_body");
    static final StandaloneModelKey<BlockStateModel> LEFT_LID_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/picnic_basket_left_lid");
    static final StandaloneModelKey<BlockStateModel> RIGHT_LID_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/picnic_basket_right_lid");

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
            int packedOverlay,
            Vec3 cameraPos) {

        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL);
        BlockStateModel leftLidModel = mc.getModelManager().getStandaloneModel(LEFT_LID_MODEL);
        BlockStateModel rightLidModel = mc.getModelManager().getStandaloneModel(RIGHT_LID_MODEL);
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
        renderQuads(consumer, poseStack.last(), bodyModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);

        // Left lid — right half of basket (x=8..13) rotates +Z away from center.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(8.0 / 16.0, 8.0 / 16.0, 8.0 / 16.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(openFraction * 90f));
        poseStack.translate(-8.0 / 16.0, -8.0 / 16.0, -8.0 / 16.0);
        renderQuads(consumer, poseStack.last(), leftLidModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Right lid — left half of basket (x=3..8) rotates −Z away from center.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(8.0 / 16.0, 8.0 / 16.0, 8.0 / 16.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-openFraction * 90f));
        poseStack.translate(-8.0 / 16.0, -8.0 / 16.0, -8.0 / 16.0);
        renderQuads(consumer, poseStack.last(), rightLidModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();
    }

    private static void renderQuads(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockStateModel model,
            Level level,
            int packedLight,
            int packedOverlay,
            RandomSource random) {
        List<BlockModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                    consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
                }
            }
            for (var quad : part.getQuads(null)) {
                Direction dir = quad.direction();
                float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
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
