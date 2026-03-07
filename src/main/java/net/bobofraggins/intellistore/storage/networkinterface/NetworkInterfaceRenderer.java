package net.bobofraggins.intellistore.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * Renders the Network Interface block as a brain-in-a-jar.
 *
 * <p>The block uses {@link net.minecraft.world.level.block.RenderShape#ENTITYBLOCK_ANIMATED},
 * so the JSON block models are empty — this BESR draws everything:
 * <ul>
 *   <li>Iron base (solid, blocky-cylinder approximation)
 *   <li>Glass cylinder + dome (translucent)
 *   <li>Blue water/fluid interior (translucent, inset)
 *   <li>Animated floating Brain item
 *   <li>Status dots (green = valid, red = invalid network)
 * </ul>
 *
 * <p>All geometry coordinates are in fractions of a block (1/16ths internally).
 * No {@code ItemBlockRenderTypes} registration is needed — using
 * {@link RenderType#translucent()} in the buffer source handles translucency in NeoForge 1.21.1.
 */
public class NetworkInterfaceRenderer implements BlockEntityRenderer<NetworkInterfaceBlockEntity> {

    // -------------------------------------------------------------------------
    // Texture locations
    // -------------------------------------------------------------------------

    private static final ResourceLocation IRON_BLOCK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/iron_block");
    private static final ResourceLocation JAR_GLASS =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/jar_glass");
    private static final ResourceLocation JAR_WATER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/jar_water");

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    public NetworkInterfaceRenderer(BlockEntityRendererProvider.Context ctx) {}

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(
            NetworkInterfaceBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        TextureAtlasSprite ironSprite = sprite(IRON_BLOCK);
        TextureAtlasSprite glassSprite = sprite(JAR_GLASS);
        TextureAtlasSprite waterSprite = sprite(JAR_WATER);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
        VertexConsumer translucent = bufferSource.getBuffer(RenderType.translucent());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Iron base — full footprint, 2/16 tall (1/8 block), solid ----
        // EPS lift off floor and inset from block edges to avoid Z-fighting.
        drawBox(
                solid,
                mat,
                EPS,
                EPS,
                EPS,
                1f - EPS,
                2f / 16,
                1f - EPS,
                ironSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Glass — single-block height, 1px inset, no bottom face ----
        float gx0 = 1f / 16, gx1 = 15f / 16;
        float gz0 = 1f / 16, gz1 = 15f / 16;
        float gy0 = 2f / 16, gy1 = 1f;
        drawBoxNoBottom(
                translucent, mat, gx0, gy0, gz0, gx1, gy1, gz1, glassSprite, 255, 255, 255, packedLight, packedOverlay);

        // ---- Water — inset 1px + EPS inside the glass on all sides ----
        // Goes all the way down to just above the base top.
        float wx0 = gx0 + 1f / 16 + EPS, wx1 = gx1 - 1f / 16 - EPS;
        float wz0 = gz0 + 1f / 16 + EPS, wz1 = gz1 - 1f / 16 - EPS;
        float wy0 = 2f / 16 + EPS, wy1 = gy1 - 1f / 16 - EPS;
        drawBox(translucent, mat, wx0, wy0, wz0, wx1, wy1, wz1, waterSprite, 255, 255, 255, packedLight, packedOverlay);

        // ---- Animated floating brain (vertical billboard quad) ----
        double time = (be.getLevel().getGameTime() + partialTick) / 20.0;
        float bob = (float) Math.sin(time * Math.PI * 0.5) * 0.04f;

        // Centre at x=0.5, z=0.5; float at 60% of block height.
        float rotY = (float) ((time * 40.0) % 360.0);
        poseStack.translate(0.5, 0.60 + bob, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotY));
        poseStack.scale(0.75f, 0.75f, 0.75f);

        // Render as a two-sided vertical quad in the X-Y plane so the brain is
        // visible from all horizontal angles as it spins around the Y axis.
        TextureAtlasSprite brainSprite = sprite(ResourceLocation.fromNamespaceAndPath("intellistore", "item/brain"));
        VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutout());
        float bu0 = brainSprite.getU0(), bu1 = brainSprite.getU1();
        float bv0 = brainSprite.getV0(), bv1 = brainSprite.getV1();
        // Front face (+Z)
        quad(
                cutout,
                mat,
                255,
                255,
                255,
                packedLight,
                packedOverlay,
                bu0,
                bv0,
                bu1,
                bv1,
                -0.5f,
                0.5f,
                0f,
                0.5f,
                0.5f,
                0f,
                0.5f,
                -0.5f,
                0f,
                -0.5f,
                -0.5f,
                0f,
                0f,
                0f,
                1f);
        // Back face (-Z) — swap x so winding faces the other way and texture isn't mirrored
        quad(
                cutout,
                mat,
                255,
                255,
                255,
                packedLight,
                packedOverlay,
                bu0,
                bv0,
                bu1,
                bv1,
                0.5f,
                0.5f,
                0f,
                -0.5f,
                0.5f,
                0f,
                -0.5f,
                -0.5f,
                0f,
                0.5f,
                -0.5f,
                0f,
                0f,
                0f,
                -1f);

        poseStack.popPose();
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
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        // -Y
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1,
                0);
        // +Y
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
        // -Z (north)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0,
                -1);
        // +Z (south)
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1);
        // -X (west)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1, 0,
                0);
        // +X (east)
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1, 0, 0);
    }

    /** Draws all faces of a box except the bottom (-Y) face. */
    private static void drawBoxNoBottom(
            VertexConsumer vc,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            TextureAtlasSprite sp,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        // +Y
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
        // -Z (north)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0,
                -1);
        // +Z (south)
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1);
        // -X (west)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1, 0,
                0);
        // +X (east)
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1, 0, 0);
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
