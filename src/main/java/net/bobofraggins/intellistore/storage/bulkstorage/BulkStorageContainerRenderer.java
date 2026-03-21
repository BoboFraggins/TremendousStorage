package net.bobofraggins.intellistore.storage.bulkstorage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;

/**
 * Renders the Bulk Storage Container using two baked models:
 * <ul>
 *   <li>{@code bulk_storage_container_body} — static body, rendered every frame
 *   <li>{@code bulk_storage_container_lid} — animated lid, rotated around the back hinge
 * </ul>
 *
 * <p>The lid hinge is at z=15/16, y=11/16 in model space. A positive X rotation opens the lid
 * (front rises up). The block uses {@link net.minecraft.world.level.block.RenderShape#ENTITYBLOCK_ANIMATED}
 * so no JSON model is drawn by the chunk renderer — this BESR draws everything.
 */
public class BulkStorageContainerRenderer
        implements BlockEntityRenderer<BulkStorageContainerBlockEntity>,
                IBlockEntityRendererExtension<BulkStorageContainerBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/bulk_storage_container_body"));
    private static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/bulk_storage_container_lid"));

    /** Y-rotation in degrees to apply for each facing direction (model default is NORTH). */
    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 90f;
            case WEST -> 270f;
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
        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel lidModel = mc.getModelManager().getModel(LID_MODEL);
        var renderer = mc.getBlockRenderer().getModelRenderer();
        var consumer = bufferSource.getBuffer(RenderType.cutoutMipped());

        Direction facing = be.getBlockState().getValue(BulkStorageContainerBlock.FACING);
        float yRot = facingYRot(facing);

        poseStack.pushPose();

        // Apply facing rotation around block centre
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Render static body
        renderer.renderModel(poseStack.last(), consumer, null, bodyModel, 1f, 1f, 1f, packedLight, packedOverlay);

        // Animate lid: pivot at back-bottom edge of lid (z=15/16, y=11/16 in model space)
        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        poseStack.translate(0.0, 11.0 / 16.0, 15.0 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -11.0 / 16.0, -15.0 / 16.0);

        // Render animated lid
        renderer.renderModel(poseStack.last(), consumer, null, lidModel, 1f, 1f, 1f, packedLight, packedOverlay);

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
