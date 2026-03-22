package net.bobofraggins.intellistore.storage.bulkstorage;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;

/**
 * Renders only the animated lid of the Bulk Storage Container.
 *
 * <p>The static body is rendered by the chunk renderer via the blockstate model
 * ({@code bulk_storage_container_body}), which gives it proper AO and face culling.
 * This BESR handles only the lid animation: pivot at z=15/16, y=11/16 in model space,
 * rotating around the X axis (positive rotation opens the lid, front rises up).
 */
public class BulkStorageContainerRenderer
        implements BlockEntityRenderer<BulkStorageContainerBlockEntity>,
                IBlockEntityRendererExtension<BulkStorageContainerBlockEntity> {

    private static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/bulk_storage_container_lid"));

    /** Y-rotation in degrees to apply for each facing direction (model default is NORTH). */
    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public BulkStorageContainerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            BulkStorageContainerBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel lidModel = mc.getModelManager().getModel(LID_MODEL);
        var renderer = mc.getBlockRenderer().getModelRenderer();
        var consumer = bufferSource.getBuffer(RenderType.solid());

        // The block is solid/opaque, so the light level at its own interior position is 0.
        // Sample from above the block to get the ambient light the lid is actually exposed to.
        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        }

        Direction facing = be.getBlockState().getValue(BulkStorageContainerBlock.FACING);
        float yRot = facingYRot(facing);

        poseStack.pushPose();

        // Match the facing rotation applied to the body by the chunk renderer
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Animate lid: pivot at back-bottom edge of lid (z=15/16, y=11/16 in model space)
        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        poseStack.translate(0.0, 11.0 / 16.0, 15.0 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -11.0 / 16.0, -15.0 / 16.0);

        int color = be.getTier().getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        // Apply the same directional shading the chunk renderer uses for the body, so the lid
        // brightness matches the body face it sits above.
        float shade = level != null ? level.getShade(facing, true) : 1f;
        renderer.renderModel(
                poseStack.last(),
                consumer,
                null,
                lidModel,
                r * shade,
                g * shade,
                b * shade,
                packedLight,
                packedOverlay);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BulkStorageContainerBlockEntity be) {
        return be.lidAngle > 0f || be.prevLidAngle > 0f;
    }

    @Override
    public AABB getRenderBoundingBox(BulkStorageContainerBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
