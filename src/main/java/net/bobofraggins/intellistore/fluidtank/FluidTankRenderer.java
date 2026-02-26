package net.bobofraggins.intellistore.fluidtank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the fluid texture in the 12×12 transparent window on the front face of the Fluid Tank.
 *
 * <p>The window occupies pixels 2–14 (block-local) on both axes of the front face. A single quad
 * is drawn just inside the face surface so it is visible through the open window in the model.
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {

    // Window bounds in block-local [0..1] coords (2px and 14px out of 16px)
    private static final float WIN_MIN = 2f / 16f;
    private static final float WIN_MAX = 14f / 16f;
    // Epsilon offset inward from the face so it doesn't z-fight with adjacent blocks
    private static final float FACE_OFFSET = 0.002f;

    public FluidTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            FluidTankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        FluidStack fluid = be.getStoredFluid();
        if (fluid.isEmpty() || be.getAmount() == 0) return;

        Direction facing = be.getBlockState().getValue(FluidTankBlock.FACING);

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTexture = ext.getStillTexture(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(stillTexture);

        int tintColor = ext.getTintColor(fluid);
        int r = (tintColor >> 16) & 0xFF;
        int g = (tintColor >> 8) & 0xFF;
        int b = tintColor & 0xFF;
        int a = (tintColor >> 24) & 0xFF;
        if (a == 0) a = 255; // opaque if no alpha component

        float u0 = sprite.getU(WIN_MIN);
        float u1 = sprite.getU(WIN_MAX);
        float v0 = sprite.getV(WIN_MIN);
        float v1 = sprite.getV(WIN_MAX);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        poseStack.pushPose();

        Matrix4f mat = poseStack.last().pose();
        // Rotate the pose so we always draw on the +Z face (north in model space), then
        // let the facing direction rotate it into world space.
        switch (facing) {
            case SOUTH -> {
                // Rotate 180° around Y
                poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.PI));
                poseStack.translate(-1, 0, -1);
            }
            case EAST -> {
                // Rotate 90° CCW around Y (east face is +X in world)
                poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.PI / 2f));
                poseStack.translate(-1, 0, 0);
            }
            case WEST -> {
                // Rotate 90° CW around Y
                poseStack.mulPose(new org.joml.Quaternionf().rotateY(-(float) Math.PI / 2f));
                poseStack.translate(0, 0, -1);
            }
            default -> {} // NORTH — no rotation needed
        }

        mat = poseStack.last().pose();
        float z = FACE_OFFSET;

        // Quad on the north (Z=0) face, window region, offset inward by FACE_OFFSET
        // Vertices: bottom-left, bottom-right, top-right, top-left (CCW from front)
        vc.addVertex(mat, WIN_MIN, WIN_MIN, z)
                .setColor(r, g, b, a)
                .setUv(u0, v1)
                .setLight(packedLight)
                .setNormal(0, 0, 1);
        vc.addVertex(mat, WIN_MAX, WIN_MIN, z)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setLight(packedLight)
                .setNormal(0, 0, 1);
        vc.addVertex(mat, WIN_MAX, WIN_MAX, z)
                .setColor(r, g, b, a)
                .setUv(u1, v0)
                .setLight(packedLight)
                .setNormal(0, 0, 1);
        vc.addVertex(mat, WIN_MIN, WIN_MAX, z)
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setLight(packedLight)
                .setNormal(0, 0, 1);

        poseStack.popPose();
    }
}
