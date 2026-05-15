package net.bobofraggins.tremendousstorage.storage.filingcabinet;

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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Renders the Filing Cabinet using two baked model parts.
 *
 * <ul>
 *   <li>{@code filing_cabinet_body} — the static outer shell.
 *   <li>{@code filing_cabinet_drawer} — the drawer (and folder tabs inside it), which slides
 *       forward 8 pixels (along the facing direction) when the UI is open, and slides closed
 *       when dismissed.
 * </ul>
 */
public class FilingCabinetRenderer
        implements BlockEntityRenderer<FilingCabinetBlockEntity>,
                IBlockEntityRendererExtension<FilingCabinetBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/filing_cabinet_body");
    static final StandaloneModelKey<BlockStateModel> DRAWER_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/filing_cabinet_drawer");

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
            int packedOverlay,
            Vec3 cameraPos) {

        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL);
        BlockStateModel drawerModel = mc.getModelManager().getStandaloneModel(DRAWER_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(FilingCabinetBlock.FACING);

        float facingYRot =
                switch (facing) {
                    case SOUTH -> 180f;
                    case EAST -> 270f;
                    case WEST -> 90f;
                    default -> 0f;
                };

        RandomSource random = RandomSource.create();

        // Static body
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        renderModel(consumer, poseStack.last(), bodyModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Sliding drawer — after facing rotation, "forward" is -Z in model space.
        float openFraction = Mth.lerp(partialTick, be.prevDrawerOffset, be.drawerOffset);
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        poseStack.translate(0.0, 0.0, -openFraction * DRAWER_SLIDE);
        renderModel(consumer, poseStack.last(), drawerModel, level, packedLight, packedOverlay, random);
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
}
