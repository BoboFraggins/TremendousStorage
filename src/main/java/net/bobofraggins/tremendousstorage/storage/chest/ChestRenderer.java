package net.bobofraggins.tremendousstorage.storage.chest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
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

public class ChestRenderer
        implements BlockEntityRenderer<ChestBlockEntity>, IBlockEntityRendererExtension<ChestBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/chest_body");
    static final StandaloneModelKey<BlockStateModel> LID_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/chest_lid");

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public ChestRenderer(BlockEntityRendererProvider.Context ctx) {}

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
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL_KEY);
        BlockStateModel lidModel = mc.getModelManager().getStandaloneModel(LID_MODEL_KEY);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(ChestBlock.FACING);
        float yRot = facingYRot(facing);

        int color = be.getTier().getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RandomSource random = RandomSource.create();

        // Body — facing rotation only.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderQuadsWithShading(
                consumer, poseStack.last(), bodyModel, level, r, g, b, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Lid — facing rotation + pivot animation.
        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0, 9.0 / 16.0, 15.0 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -9.0 / 16.0, -15.0 / 16.0);
        renderQuadsWithShading(
                consumer, poseStack.last(), lidModel, level, r, g, b, packedLight, packedOverlay, random);
        poseStack.popPose();
    }

    private static void renderQuadsWithShading(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockStateModel model,
            Level level,
            float r,
            float g,
            float b,
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
