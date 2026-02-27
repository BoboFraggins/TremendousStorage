package net.bobofraggins.intellistore.networkinterface;

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
 * Renders the Network Interface block (lower + upper half combined).
 *
 * <p>The BESR is registered for the lower-half {@link BlockEntityType} only.
 * It draws:
 * <ul>
 *   <li><b>Lower body</b> — a full cube using the {@code network_interface} placeholder texture
 *   <li><b>Upper body</b> — a full cube using the vanilla glass texture (translated +1 Y)
 *   <li><b>Status dots</b> — a small 4×4-pixel square on each of the 6 lower-half faces,
 *       green when the network is valid, red otherwise
 * </ul>
 */
public class NetworkInterfaceRenderer implements BlockEntityRenderer<NetworkInterfaceBlockEntity> {

    private static final ResourceLocation NI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/network_interface");
    private static final ResourceLocation GLASS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/glass");

    // Status dot: centered 4/16 × 4/16 square, inset 0.002 from the face surface
    private static final float DOT_MIN  = 6f / 16f;
    private static final float DOT_MAX  = 10f / 16f;
    private static final float DOT_INSET = 0.002f;

    public NetworkInterfaceRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(NetworkInterfaceBlockEntity be, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        TextureAtlasSprite niSprite = Minecraft.getInstance()
                .getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(NI_TEXTURE);
        TextureAtlasSprite glassSprite = Minecraft.getInstance()
                .getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(GLASS_TEXTURE);

        VertexConsumer solid   = bufferSource.getBuffer(RenderType.solid());
        VertexConsumer cutout  = bufferSource.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Lower body (full cube 0→1 in block-local space) ----
        drawBox(solid, mat, 0, 0, 0, 1, 1, 1,
                niSprite, 255, 255, 255, packedLight, packedOverlay);

        // ---- Upper body (translate +1 in Y) ----
        poseStack.translate(0, 1, 0);
        mat = poseStack.last().pose();
        drawBox(cutout, mat, 0, 0, 0, 1, 1, 1,
                glassSprite, 255, 255, 255, packedLight, packedOverlay);
        poseStack.translate(0, -1, 0);
        mat = poseStack.last().pose();

        // ---- Status dots on each face of the lower body ----
        boolean valid = be.isNetworkValid();
        int r = valid ? 0   : 220;
        int g = valid ? 200 : 20;
        int b = valid ? 0   : 20;

        // -Y (down face): dot at y=0 plane, raised by DOT_INSET
        dot(solid, mat, r, g, b, packedLight, packedOverlay,
                DOT_MIN, DOT_INSET, DOT_MAX,
                DOT_MAX, DOT_INSET, DOT_MAX,
                DOT_MAX, DOT_INSET, DOT_MIN,
                DOT_MIN, DOT_INSET, DOT_MIN,
                0, -1, 0);
        // +Y (up face): dot at y=1 plane, lowered by DOT_INSET
        dot(solid, mat, r, g, b, packedLight, packedOverlay,
                DOT_MIN, 1f - DOT_INSET, DOT_MIN,
                DOT_MAX, 1f - DOT_INSET, DOT_MIN,
                DOT_MAX, 1f - DOT_INSET, DOT_MAX,
                DOT_MIN, 1f - DOT_INSET, DOT_MAX,
                0, 1, 0);
        // -Z (north face): dot at z=0 plane, pushed by DOT_INSET
        dot(solid, mat, r, g, b, packedLight, packedOverlay,
                DOT_MAX, DOT_MAX, DOT_INSET,
                DOT_MIN, DOT_MAX, DOT_INSET,
                DOT_MIN, DOT_MIN, DOT_INSET,
                DOT_MAX, DOT_MIN, DOT_INSET,
                0, 0, -1);
        // +Z (south face): dot at z=1 plane, pulled by DOT_INSET
        dot(solid, mat, r, g, b, packedLight, packedOverlay,
                DOT_MIN, DOT_MAX, 1f - DOT_INSET,
                DOT_MAX, DOT_MAX, 1f - DOT_INSET,
                DOT_MAX, DOT_MIN, 1f - DOT_INSET,
                DOT_MIN, DOT_MIN, 1f - DOT_INSET,
                0, 0, 1);
        // -X (west face): dot at x=0 plane, pushed by DOT_INSET
        dot(solid, mat, r, g, b, packedLight, packedOverlay,
                DOT_INSET, DOT_MAX, DOT_MIN,
                DOT_INSET, DOT_MAX, DOT_MAX,
                DOT_INSET, DOT_MIN, DOT_MAX,
                DOT_INSET, DOT_MIN, DOT_MIN,
                -1, 0, 0);
        // +X (east face): dot at x=1 plane, pulled by DOT_INSET
        dot(solid, mat, r, g, b, packedLight, packedOverlay,
                1f - DOT_INSET, DOT_MAX, DOT_MAX,
                1f - DOT_INSET, DOT_MAX, DOT_MIN,
                1f - DOT_INSET, DOT_MIN, DOT_MIN,
                1f - DOT_INSET, DOT_MIN, DOT_MAX,
                1, 0, 0);

        poseStack.popPose();
    }

    // -------------------------------------------------------------------------
    // Geometry helpers — same pattern as TubeRenderer
    // -------------------------------------------------------------------------

    private static void drawBox(VertexConsumer vc, Matrix4f mat,
            float x0, float y0, float z0, float x1, float y1, float z1,
            TextureAtlasSprite sprite, int r, int g, int b, int light, int overlay) {

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        // -Y
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1,
                x0, y0, z1,  x1, y0, z1,  x1, y0, z0,  x0, y0, z0,  0, -1, 0);
        // +Y
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1,
                x0, y1, z0,  x1, y1, z0,  x1, y1, z1,  x0, y1, z1,  0, 1, 0);
        // -Z
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1,
                x1, y1, z0,  x0, y1, z0,  x0, y0, z0,  x1, y0, z0,  0, 0, -1);
        // +Z
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1,
                x0, y1, z1,  x1, y1, z1,  x1, y0, z1,  x0, y0, z1,  0, 0, 1);
        // -X
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1,
                x0, y1, z0,  x0, y1, z1,  x0, y0, z1,  x0, y0, z0,  -1, 0, 0);
        // +X
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1,
                x1, y1, z1,  x1, y1, z0,  x1, y0, z0,  x1, y0, z1,  1, 0, 0);
    }

    /** Draws a single flat quad (for a status dot). */
    private static void dot(VertexConsumer vc, Matrix4f mat,
            int r, int g, int b, int light, int overlay,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz) {
        // Use a 1×1 UV (solid color via tint)
        float u = 0.5f, v = 0.5f;
        vc.addVertex(mat, x0, y0, z0).setColor(r, g, b, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(r, g, b, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(r, g, b, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
            int r, int g, int b, int light, int overlay,
            float u0, float v0, float u1, float v1,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz) {
        vc.addVertex(mat, x0, y0, z0).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }
}
