package net.bobofraggins.intellistore.filingcabinet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the Filing Cabinet block entity: the animated drawer model and the folder items
 * stacked inside the open drawer.
 *
 * <p>The static cabinet faces (top, sides, back) are rendered by the JSON block model via the
 * normal chunk bake; this renderer draws only the parts that need per-BE data or animation.
 *
 * <p>Coordinate system matches RFC's original renderer:
 * <ul>
 *   <li>Origin is block centre.
 *   <li>Y is flipped 180° (model was built upside-down in the original 1.12 tool).
 *   <li>Y rotation matches the FACING direction.
 * </ul>
 */
public class FilingCabinetRenderer implements BlockEntityRenderer<FilingCabinetBlockEntity> {

    /** Scale factor matching RFC's 1/16-block model unit. */
    private static final float F = 0.0625f;

    private final FilingCabinetModel model;
    private final ItemRenderer itemRenderer;

    public FilingCabinetRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(FilingCabinetModel.LAYER);
        this.model = new FilingCabinetModel(root);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(
            FilingCabinetBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(FilingCabinetBlock.FACING);

        // Interpolate drawer offset between previous and current tick values
        float drawerOffset = be.prevDrawerOffset
                + (be.drawerOffset - be.prevDrawerOffset) * partialTick;

        poseStack.pushPose();

        // Move to block centre
        poseStack.translate(0.5, 0.5, 0.5);
        // RFC model was built upside-down
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        // Rotate to match FACING
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));

        // --- Render folder items when the drawer is not fully closed ---
        boolean drawerVisible = be.isOpen() || drawerOffset < FilingCabinetBlockEntity.OFFSET_CLOSED;
        if (drawerVisible) {
            poseStack.pushPose();
            // Base position of the folder stack inside the drawer (matches RFC offsets)
            poseStack.translate(-0.4, 0.3, drawerOffset + 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));

            for (int i = 0; i < FilingCabinetBlockEntity.SLOT_COUNT; i++) {
                ItemStack folder = be.getFolder(i);
                if (folder.isEmpty()) continue;

                // Each folder is spaced slightly in Z (depth), mimicking a stack of paper
                poseStack.translate(0, -0.0025, 0.1);
                renderFolder(folder, poseStack, bufferSource, packedLight, packedOverlay);
            }
            poseStack.popPose();
        }

        // --- Render the cabinet model (body + animated drawer) ---
        poseStack.scale(F, F, F);
        var consumer = bufferSource.getBuffer(RenderType.entitySolid(FilingCabinetModel.TEXTURE));
        model.renderAll(poseStack, consumer, packedLight, packedOverlay, drawerOffset / F);

        poseStack.popPose();
    }

    private void renderFolder(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        poseStack.pushPose();
        // Local offset and scale to sit the folder nicely inside the drawer
        poseStack.translate(0.4, 0.45, 0.1);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.NONE,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be_world(stack),
                0);
        poseStack.popPose();
    }

    /** Returns null — item renderer accepts null world for static display context. */
    private static net.minecraft.world.level.Level be_world(ItemStack ignored) {
        return null;
    }
}
