package net.bobofraggins.intellistore.storage.wirelesshub;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Renders the Wireless Hub as a Jacob's Ladder — two metal rods rising from an iron base,
 * with an animated electric arc climbing between them.
 *
 * <p>Rendering layers (all drawn by this BESR; JSON model provides particle texture only):
 * <ol>
 *   <li>Iron base — solid, 8×8×2 px slab centred on the block.
 *   <li>Two vertical iron rods — solid, 1×12×1 px each, separated by 5px.
 *   <li>Animated arc — translucent blue quad-strip that climbs from rod-bottom to rod-top
 *       over a 40-tick (2 s) cycle, with random per-frame zigzag offsets.
 * </ol>
 *
 * <p>An {@link ParticleTypes#ELECTRIC_SPARK} burst fires at the rod tips when the arc
 * reaches the top of its climb.
 */
public class WirelessHubRenderer implements BlockEntityRenderer<WirelessHubBlockEntity> {

    // -------------------------------------------------------------------------
    // Textures
    // -------------------------------------------------------------------------

    private static final ResourceLocation IRON_BLOCK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/iron_block");

    // -------------------------------------------------------------------------
    // Geometry constants — all in block units (0..1), matching the JSON model
    // -------------------------------------------------------------------------

    /** Small offset to prevent Z-fighting with the ground block. */
    private static final float EPS = 1e-4f;

    // Base slab: 8×2×8 px centred
    private static final float BASE_X0 = 4f / 16;
    private static final float BASE_X1 = 12f / 16;
    private static final float BASE_Y0 = EPS; // raised off floor to prevent Z-fighting
    private static final float BASE_Y1 = 2f / 16;
    private static final float BASE_Z0 = 4f / 16;
    private static final float BASE_Z1 = 12f / 16;

    // Left rod: x 5-6, z 8-9, y 2-14
    private static final float LROD_X0 = 5f / 16;
    private static final float LROD_X1 = 6f / 16;
    private static final float ROD_Z0 = 8f / 16;
    private static final float ROD_Z1 = 9f / 16;
    private static final float ROD_Y0 = 2f / 16;
    private static final float ROD_Y1 = 14f / 16;

    // Right rod: x 10-11, same z and y
    private static final float RROD_X0 = 10f / 16;
    private static final float RROD_X1 = 11f / 16;

    // Arc anchor X positions (centre of each rod face that faces the other)
    private static final float ARC_LEFT_X = 6f / 16; // right face of left rod
    private static final float ARC_RIGHT_X = 10f / 16; // left face of right rod
    private static final float ARC_Z = 8.5f / 16; // midpoint of rod depth

    // Arc Y range
    private static final float ARC_BOTTOM_Y = ROD_Y0;
    private static final float ARC_TOP_Y = ROD_Y1;

    // Half-width of the arc quad strip
    private static final float ARC_HALF_W = 0.5f / 16;

    // Number of zigzag segments per arc
    private static final int ARC_SEGMENTS = 8;

    // Arc colour (electric blue)
    private static final int ARC_R = 100;
    private static final int ARC_G = 200;
    private static final int ARC_B = 255;
    private static final int ARC_A = 220;

    public WirelessHubRenderer(BlockEntityRendererProvider.Context ctx) {}

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(
            WirelessHubBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        TextureAtlasSprite iron = sprite(IRON_BLOCK);
        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Iron base ----
        drawBox(
                solid,
                mat,
                BASE_X0,
                BASE_Y0,
                BASE_Z0,
                BASE_X1,
                BASE_Y1,
                BASE_Z1,
                iron,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Left rod ----
        drawBox(
                solid,
                mat,
                LROD_X0,
                ROD_Y0,
                ROD_Z0,
                LROD_X1,
                ROD_Y1,
                ROD_Z1,
                iron,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Right rod ----
        drawBox(
                solid,
                mat,
                RROD_X0,
                ROD_Y0,
                ROD_Z0,
                RROD_X1,
                ROD_Y1,
                ROD_Z1,
                iron,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Animated arc ----
        float progress = be.getAnimationProgress(partialTick);
        float arcTopY = ARC_BOTTOM_Y + (ARC_TOP_Y - ARC_BOTTOM_Y) * progress;

        // Seed changes each tick so the zigzag reshuffles; use tickCount embedded in
        // getAnimationProgress by deriving seed from progress stepping.
        long seed = (long) (progress * 1000) * 0x9e3779b97f4a7c15L;
        Random rand = new Random(seed);

        VertexConsumer lightning = bufferSource.getBuffer(RenderType.lightning());

        // Build array of (x, y) knot points from bottom to arcTopY
        float[] knotX = new float[ARC_SEGMENTS + 1];
        float[] knotY = new float[ARC_SEGMENTS + 1];

        knotX[0] = ARC_LEFT_X;
        knotY[0] = ARC_BOTTOM_Y;

        for (int i = 1; i <= ARC_SEGMENTS; i++) {
            float t = i / (float) ARC_SEGMENTS;
            // X travels from left rod to right rod
            float baseX = ARC_LEFT_X + (ARC_RIGHT_X - ARC_LEFT_X) * t;
            // Random lateral zigzag
            float jitter = (rand.nextFloat() - 0.5f) * 0.12f;
            knotX[i] = baseX + jitter;
            knotY[i] = ARC_BOTTOM_Y + (arcTopY - ARC_BOTTOM_Y) * t;
        }

        // Emit as quad strip: each segment = a thin quad, drawn in Z-axis plane
        for (int i = 0; i < ARC_SEGMENTS; i++) {
            float x0 = knotX[i];
            float y0 = knotY[i];
            float x1 = knotX[i + 1];
            float y1 = knotY[i + 1];
            float z = ARC_Z;

            // Quad vertices: bottom-left, bottom-right, top-right, top-left
            lightning
                    .addVertex(mat, x0 - ARC_HALF_W, y0, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(0, 0)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0) // full brightness — arc glows
                    .setNormal(0, 0, 1);
            lightning
                    .addVertex(mat, x0 + ARC_HALF_W, y0, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(1, 0)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);
            lightning
                    .addVertex(mat, x1 + ARC_HALF_W, y1, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(1, 1)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);
            lightning
                    .addVertex(mat, x1 - ARC_HALF_W, y1, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(0, 1)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);
        }

        poseStack.popPose();

        // ---- Spark particle at the top of the arc when it reaches the peak ----
        if (progress > 0.9f) {
            Level level = be.getLevel();
            if (level != null && level.isClientSide()) {
                double wx = be.getBlockPos().getX() + 0.5;
                double wy = be.getBlockPos().getY() + ARC_TOP_Y;
                double wz = be.getBlockPos().getZ() + ARC_Z;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, wx, wy, wz, 0, 0.02, 0);
            }
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
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        // -Y (bottom)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1,
                0);
        // +Y (top)
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
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, 255)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, 255)
                .setUv(u1, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, 255)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, 255)
                .setUv(u0, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
