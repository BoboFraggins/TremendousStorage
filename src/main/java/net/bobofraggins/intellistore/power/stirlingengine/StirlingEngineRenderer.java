package net.bobofraggins.intellistore.power.stirlingengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * Renders the Stirling Engine as a blocky base with a cylindrical top.
 *
 * <p>When heated, a spinning flywheel disc rotates at the top.
 * The body uses an iron texture for the base and a metal/copper texture for the cylinder.
 * The flywheel spins on the Y axis.
 */
public class StirlingEngineRenderer implements BlockEntityRenderer<StirlingEngineBlockEntity> {

    private static final ResourceLocation IRON_BLOCK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/iron_block");
    private static final ResourceLocation COPPER_BLOCK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/copper_block");
    private static final ResourceLocation CAMPFIRE_LOG =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/campfire_log");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    public StirlingEngineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            StirlingEngineBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        TextureAtlasSprite ironSprite = sprite(IRON_BLOCK);
        TextureAtlasSprite metalSprite = sprite(COPPER_BLOCK);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Base: 3/4 x 3/4 footprint, 6px tall ----
        // From [2,EPS,2] to [14,6,14] in 1/16 coords; EPS avoids Z-fighting with ground block.
        drawBox(solid, mat, 2f / 16, EPS, 2f / 16, 14f / 16, 6f / 16, 14f / 16, ironSprite, packedLight, packedOverlay);

        // ---- Cylinder: narrow upright column, 6px wide, 10px tall, centered ----
        // Core: [5,6,5] to [11,16,11]
        drawBox(solid, mat, 5f / 16, 6f / 16, 5f / 16, 11f / 16, 1f, 11f / 16, metalSprite, packedLight, packedOverlay);

        // Rounded-cylinder illusion: two cross-pieces
        // [4,6,6] to [12,16,10]
        drawBox(solid, mat, 4f / 16, 6f / 16, 6f / 16, 12f / 16, 1f, 10f / 16, metalSprite, packedLight, packedOverlay);
        // [6,6,4] to [10,16,12]
        drawBox(solid, mat, 6f / 16, 6f / 16, 4f / 16, 10f / 16, 1f, 12f / 16, metalSprite, packedLight, packedOverlay);

        poseStack.popPose();

        // ---- Flywheel — only when heated ----
        if (be.isHeated()) {
            poseStack.pushPose();

            // Position flywheel at the top centre of the cylinder (y=1.0, top of block)
            poseStack.translate(0.5, 1f - EPS, 0.5);

            float animTick = be.animationTicks + partialTick * 1.5f;
            float rotation = (animTick * 10f) % 360f;
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            VertexConsumer fw = bufferSource.getBuffer(RenderType.solid());
            Matrix4f fwMat = poseStack.last().pose();
            TextureAtlasSprite fwSprite = sprite(CAMPFIRE_LOG);

            // Horizontal disc: two perpendicular flat quads
            float r = 5f / 16f, thick = 1f / 16f;
            drawFlatQuadY(fw, fwMat, -r, thick / 2, -r, r, thick / 2, r, fwSprite, packedLight, packedOverlay);
            drawFlatQuadY(fw, fwMat, -r, -thick / 2, -r, r, -thick / 2, r, fwSprite, packedLight, packedOverlay);

            poseStack.popPose();
        }
    }

    // -------------------------------------------------------------------------
    // Geometry helpers
    // -------------------------------------------------------------------------

    private static TextureAtlasSprite sprite(ResourceLocation loc) {
        return Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(loc);
    }

    private static void drawBox(
            VertexConsumer vc,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            TextureAtlasSprite sp,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        int r = 255, g = 255, b = 255;
        // -Y
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1,
                0);
        // +Y
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
        // -Z
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0,
                -1);
        // +Z
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1);
        // -X
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1, 0,
                0);
        // +X
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1, 0, 0);
    }

    /** Draws a horizontal flat quad (top face only) for the flywheel. */
    private static void drawFlatQuadY(
            VertexConsumer vc,
            Matrix4f mat,
            float x0,
            float y,
            float z0,
            float x1,
            float dummy,
            float z1,
            TextureAtlasSprite sp,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        int r = 180, g = 180, b = 180;
        float ny = y > 0 ? 1 : -1;
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1, 0, ny, 0);
    }

    private static void quad(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int light,
            int overlay,
            float u0,
            float v0,
            float u1,
            float v1,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz) {
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, 255)
                .setUv(u0, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, 255)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, 255)
                .setUv(u1, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, 255)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
