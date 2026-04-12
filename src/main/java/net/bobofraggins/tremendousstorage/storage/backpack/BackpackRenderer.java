package net.bobofraggins.tremendousstorage.storage.backpack;

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
 * Renders the body and animated flap of the placed Tremendous Backpack.
 *
 * <p>Follows the same BakedModel + PoseStack pivot pattern as
 * {@link net.bobofraggins.tremendousstorage.storage.chest.ChestRenderer}.
 *
 * <p>Flap pivot: back-top edge of the main body at (z = 12.251/16, y = 12/16).
 * Opening rotates around the X-axis by up to 90°.
 *
 * <p>Uses {@link ChestBlockEntity} as the type bound so it can also render the
 * Ender Tremendous Backpack block entity, which extends the chest block entity hierarchy.
 */
public class BackpackRenderer
        implements BlockEntityRenderer<ChestBlockEntity>, IBlockEntityRendererExtension<ChestBlockEntity> {

    static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/backpack_body"));
    static final ModelResourceLocation FLAP_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/backpack_flap"));

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public BackpackRenderer(BlockEntityRendererProvider.Context ctx) {}

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
        BakedModel flapModel = mc.getModelManager().getModel(FLAP_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(BackpackBlock.FACING);
        float yRot = facingYRot(facing);

        RandomSource random = RandomSource.create();

        // Body — facing rotation only.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderQuads(consumer, poseStack.last(), bodyModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Flap — facing rotation + pivot animation at back-top edge of body.
        // Pivot: z = 12.251/16, y = 12/16.  Rotates +X to open (swings forward).
        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0, 12.0 / 16.0, 12.251 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -12.0 / 16.0, -12.251 / 16.0);
        renderQuads(consumer, poseStack.last(), flapModel, blockState, level, packedLight, packedOverlay, random);
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
