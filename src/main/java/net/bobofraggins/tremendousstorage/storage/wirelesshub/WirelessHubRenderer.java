package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Renders the Wireless Hub as a Jacob's Ladder — two Lazurite rods rising from a Lazurite base,
 * with an animated electric arc climbing between them.
 *
 * <p>Rendering layers (all drawn by this BESR; JSON model provides particle texture only):
 * <ol>
 *   <li>Lazurite base — solid, 8×8×2 px slab centred on the block.
 *   <li>Two vertical Lazurite rods — solid, 1×12×1 px each, separated by 5px.
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

    private static final ResourceLocation LAZURITE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/lazurite_block");

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

    // V-shape rods: both start at block-centre X at the base, fan outward toward the top.
    private static final float ROD_Y0 = 2f / 16;
    private static final float ROD_Y1 = 14f / 16;
    private static final float ROD_BASE_X = 8f / 16; // shared X origin at base (block centre)
    private static final float LROD_TOP_CX = 4.5f / 16; // left rod centre-X at top
    private static final float RROD_TOP_CX = 11.5f / 16; // right rod centre-X at top
    private static final float ROD_HALF_W = 0.5f / 16; // 1 px half-width

    private static final float ARC_Z = 8.5f / 16; // midpoint of rod depth

    // Arc inner-face X at full spread (top of V); lerped by progress during animation.
    private static final float ARC_LEFT_X_TOP = LROD_TOP_CX + ROD_HALF_W; // = 5/16
    private static final float ARC_RIGHT_X_TOP = RROD_TOP_CX - ROD_HALF_W; // = 11/16

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

        TextureAtlasSprite iron = sprite(LAZURITE_BLOCK);
        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();

        // Rotate geometry to match the placed facing direction.
        // Default geometry (SOUTH = 0°): rods separated along X, arc visible from +Z face.
        Direction facing = be.getBlockState().getValue(WirelessHubBlock.FACING);
        float yDeg =
                switch (facing) {
                    case WEST -> 90f;
                    case NORTH -> 180f;
                    case EAST -> 270f;
                    default -> 0f; // SOUTH
                };
        if (yDeg != 0f) {
            poseStack.translate(0.5f, 0f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(yDeg));
            poseStack.translate(-0.5f, 0f, -0.5f);
        }

        Matrix4f mat = poseStack.last().pose();

        // ---- Lazurite base ----
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

        // ---- V-shape rods (diagonal cross-quads) ----
        drawDiagonalRod(
                solid,
                mat,
                ROD_BASE_X,
                ROD_Y0,
                LROD_TOP_CX,
                ROD_Y1,
                ARC_Z,
                ROD_HALF_W,
                iron,
                packedLight,
                packedOverlay);
        drawDiagonalRod(
                solid,
                mat,
                ROD_BASE_X,
                ROD_Y0,
                RROD_TOP_CX,
                ROD_Y1,
                ARC_Z,
                ROD_HALF_W,
                iron,
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

        // Arc X anchors widen with progress — narrow at the base of the V, full-width at the top.
        float arcLeftX = ROD_BASE_X + (ARC_LEFT_X_TOP - ROD_BASE_X) * progress;
        float arcRightX = ROD_BASE_X + (ARC_RIGHT_X_TOP - ROD_BASE_X) * progress;

        // Both endpoints sit at arcTopY so both rods rise together.
        // Interior knots jitter in X and Y around that height.
        float[] knotX = new float[ARC_SEGMENTS + 1];
        float[] knotY = new float[ARC_SEGMENTS + 1];

        knotX[0] = arcLeftX;
        knotY[0] = arcTopY;
        for (int i = 1; i < ARC_SEGMENTS; i++) {
            float t = i / (float) ARC_SEGMENTS;
            float xJitter = (rand.nextFloat() - 0.5f) * 0.12f;
            float yJitter = (rand.nextFloat() - 0.5f) * 0.06f;
            knotX[i] = arcLeftX + (arcRightX - arcLeftX) * t + xJitter;
            knotY[i] = arcTopY + yJitter;
        }
        knotX[ARC_SEGMENTS] = arcRightX;
        knotY[ARC_SEGMENTS] = arcTopY;

        // Emit each segment in two perpendicular planes (cross) so the arc is
        // visible from any horizontal viewing angle, regardless of hub facing.
        for (int i = 0; i < ARC_SEGMENTS; i++) {
            float xa = knotX[i], ya = knotY[i];
            float xb = knotX[i + 1], yb = knotY[i + 1];
            float z = ARC_Z;

            // Plane 1: width along X — face visible from north/south (±Z axis)
            lightning
                    .addVertex(mat, xa - ARC_HALF_W, ya, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(0, 0)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);
            lightning
                    .addVertex(mat, xa + ARC_HALF_W, ya, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(1, 0)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);
            lightning
                    .addVertex(mat, xb + ARC_HALF_W, yb, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(1, 1)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);
            lightning
                    .addVertex(mat, xb - ARC_HALF_W, yb, z)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(0, 1)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(0, 0, 1);

            // Plane 2: width along Z — face visible from east/west (±X axis)
            lightning
                    .addVertex(mat, xa, ya, z - ARC_HALF_W)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(0, 0)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(1, 0, 0);
            lightning
                    .addVertex(mat, xa, ya, z + ARC_HALF_W)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(1, 0)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(1, 0, 0);
            lightning
                    .addVertex(mat, xb, yb, z + ARC_HALF_W)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(1, 1)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(1, 0, 0);
            lightning
                    .addVertex(mat, xb, yb, z - ARC_HALF_W)
                    .setColor(ARC_R, ARC_G, ARC_B, ARC_A)
                    .setUv(0, 1)
                    .setOverlay(packedOverlay)
                    .setLight(0xF000F0)
                    .setNormal(1, 0, 0);
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

    /**
     * Draws a diagonal rod as two perpendicular cross-quad planes (front + back each), going from
     * (x0, y0) at the base to (x1, y1) at the tip, centred on z.
     */
    private static void drawDiagonalRod(
            VertexConsumer vc,
            Matrix4f mat,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            float hw,
            TextureAtlasSprite sp,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        // XY-plane — visible from ±Z
        quad(
                vc, mat, 255, 255, 255, light, overlay, u0, v0, u1, v1, x0 - hw, y0, z, x0 + hw, y0, z, x1 + hw, y1, z,
                x1 - hw, y1, z, 0, 0, 1);
        quad(
                vc, mat, 255, 255, 255, light, overlay, u0, v0, u1, v1, x0 + hw, y0, z, x0 - hw, y0, z, x1 - hw, y1, z,
                x1 + hw, y1, z, 0, 0, -1);
        // ZY-plane — visible from ±X
        quad(
                vc, mat, 255, 255, 255, light, overlay, u0, v0, u1, v1, x0, y0, z + hw, x0, y0, z - hw, x1, y1, z - hw,
                x1, y1, z + hw, 1, 0, 0);
        quad(
                vc, mat, 255, 255, 255, light, overlay, u0, v0, u1, v1, x0, y0, z - hw, x0, y0, z + hw, x1, y1, z + hw,
                x1, y1, z - hw, -1, 0, 0);
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
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1,
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
