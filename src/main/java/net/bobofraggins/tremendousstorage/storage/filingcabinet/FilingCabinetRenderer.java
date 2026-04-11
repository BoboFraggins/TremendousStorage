package net.bobofraggins.tremendousstorage.storage.filingcabinet;

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
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders the Filing Cabinet using two baked model parts.
 *
 * <ul>
 *   <li>{@code filing_cabinet_body} — the static outer shell.
 *   <li>{@code filing_cabinet_drawer} — the drawer (and folder tabs inside it), which slides
 *       forward 8 pixels (along the facing direction) when the UI is open, and slides closed
 *       when dismissed. The animation runs at the same rate as the vanilla chest lid (0.1
 *       per tick), interpolated with {@code partialTick} for smooth motion.
 * </ul>
 */
public class FilingCabinetRenderer implements BlockEntityRenderer<FilingCabinetBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/filing_cabinet_body"));
    private static final ModelResourceLocation DRAWER_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/filing_cabinet_drawer"));

    /** Maximum drawer slide distance in block units. */
    private static final float DRAWER_SLIDE = 8f / 16f;

    public FilingCabinetRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            FilingCabinetBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel drawerModel = mc.getModelManager().getModel(DRAWER_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(FilingCabinetBlock.FACING);

        // The model's default facing is NORTH (front panel at Z=0).
        float facingYRot = switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f; // NORTH
        };

        RandomSource random = RandomSource.create();

        // ---- Static body ----
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        renderModel(consumer, poseStack.last(), bodyModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // ---- Sliding drawer ----
        float openFraction = Mth.lerp(partialTick, be.prevDrawerOffset, be.drawerOffset);
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        // After facing rotation, "forward" (toward the player) is -Z in model space.
        poseStack.translate(0.0, 0.0, -openFraction * DRAWER_SLIDE);
        renderModel(consumer, poseStack.last(), drawerModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();
    }

    private static void applyFacingRotation(PoseStack poseStack, float yDeg) {
        if (yDeg != 0f) {
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(yDeg));
            poseStack.translate(-0.5, -0.5, -0.5);
        }
    }

    private static void renderModel(
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
}
