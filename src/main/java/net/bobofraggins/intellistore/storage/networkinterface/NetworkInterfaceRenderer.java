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
 * The upper half is rendered by pushing +1 Y onto the pose stack.
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
    private static final ResourceLocation BRAIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/brain");

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    private static final float DOT_MIN = 6f / 16f;
    private static final float DOT_MAX = 10f / 16f;
    private static final float DOT_INSET = EPS;

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

        // ---- Iron base — solid, blocky-cylinder (three overlapping boxes) ----
        // y starts at EPS above floor to avoid Z-fighting with the ground block.
        // The centre box is raised EPS taller so its top face sits just above the two
        // arm boxes, preventing Z-fighting on the coplanar top surfaces.
        drawBox(
                solid,
                mat,
                3f / 16,
                EPS,
                3f / 16,
                13f / 16,
                4f / 16 + EPS,
                13f / 16,
                ironSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawBox(
                solid,
                mat,
                2f / 16,
                EPS,
                4f / 16,
                14f / 16,
                4f / 16,
                12f / 16,
                ironSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawBox(
                solid,
                mat,
                4f / 16,
                EPS,
                2f / 16,
                12f / 16,
                4f / 16,
                14f / 16,
                ironSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Water interior — single tall box spanning both blocks ----
        // Drawn here (lower-block pose) so there is no seam. Runs from top of base
        // (5/16) up to dome base (1 + 14/16 = 30/16).
        drawBox(
                translucent,
                mat,
                4f / 16,
                5f / 16,
                4f / 16,
                12f / 16,
                30f / 16,
                12f / 16,
                waterSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Glass cylinder — exterior-only faces of the octagonal cross silhouette ----
        // Drawn as individual quads so no interior faces are emitted, eliminating Z-fighting
        // and double-blending at the overlap seams between the three component boxes.
        // y range: base top (4/16) to dome base (1 + 14/16 = 30/16).
        drawGlassCylinder(translucent, mat, glassSprite, packedLight, packedOverlay);

        // ---- Status dots on the lower body faces ----
        boolean valid = be.isNetworkValid();
        int r = valid ? 0 : 220;
        int g = valid ? 200 : 20;
        int b = valid ? 0 : 20;

        // -Y (down)
        dot(
                solid,
                mat,
                r,
                g,
                b,
                packedLight,
                packedOverlay,
                DOT_MIN,
                DOT_INSET,
                DOT_MAX,
                DOT_MAX,
                DOT_INSET,
                DOT_MAX,
                DOT_MAX,
                DOT_INSET,
                DOT_MIN,
                DOT_MIN,
                DOT_INSET,
                DOT_MIN,
                0,
                -1,
                0);
        // +Y (up)
        dot(
                solid,
                mat,
                r,
                g,
                b,
                packedLight,
                packedOverlay,
                DOT_MIN,
                1f - DOT_INSET,
                DOT_MIN,
                DOT_MAX,
                1f - DOT_INSET,
                DOT_MIN,
                DOT_MAX,
                1f - DOT_INSET,
                DOT_MAX,
                DOT_MIN,
                1f - DOT_INSET,
                DOT_MAX,
                0,
                1,
                0);
        // -Z (north)
        dot(
                solid,
                mat,
                r,
                g,
                b,
                packedLight,
                packedOverlay,
                DOT_MAX,
                DOT_MAX,
                DOT_INSET,
                DOT_MIN,
                DOT_MAX,
                DOT_INSET,
                DOT_MIN,
                DOT_MIN,
                DOT_INSET,
                DOT_MAX,
                DOT_MIN,
                DOT_INSET,
                0,
                0,
                -1);
        // +Z (south)
        dot(
                solid,
                mat,
                r,
                g,
                b,
                packedLight,
                packedOverlay,
                DOT_MIN,
                DOT_MAX,
                1f - DOT_INSET,
                DOT_MAX,
                DOT_MAX,
                1f - DOT_INSET,
                DOT_MAX,
                DOT_MIN,
                1f - DOT_INSET,
                DOT_MIN,
                DOT_MIN,
                1f - DOT_INSET,
                0,
                0,
                1);
        // -X (west)
        dot(
                solid,
                mat,
                r,
                g,
                b,
                packedLight,
                packedOverlay,
                DOT_INSET,
                DOT_MAX,
                DOT_MIN,
                DOT_INSET,
                DOT_MAX,
                DOT_MAX,
                DOT_INSET,
                DOT_MIN,
                DOT_MAX,
                DOT_INSET,
                DOT_MIN,
                DOT_MIN,
                -1,
                0,
                0);
        // +X (east)
        dot(
                solid,
                mat,
                r,
                g,
                b,
                packedLight,
                packedOverlay,
                1f - DOT_INSET,
                DOT_MAX,
                DOT_MAX,
                1f - DOT_INSET,
                DOT_MAX,
                DOT_MIN,
                1f - DOT_INSET,
                DOT_MIN,
                DOT_MIN,
                1f - DOT_INSET,
                DOT_MIN,
                DOT_MAX,
                1,
                0,
                0);

        // ---- Upper half: translate +1 Y ----
        poseStack.translate(0, 1, 0);
        mat = poseStack.last().pose();

        // Glass dome — three overlapping boxes tapering to top
        drawBox(
                translucent,
                mat,
                5f / 16,
                14f / 16,
                5f / 16,
                11f / 16,
                1f,
                11f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawBox(
                translucent,
                mat,
                4f / 16,
                15f / 16,
                6f / 16,
                12f / 16,
                1f,
                10f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawBox(
                translucent,
                mat,
                6f / 16,
                15f / 16,
                4f / 16,
                10f / 16,
                1f,
                12f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Animated floating brain (3D geometry) ----
        // Use game time for smooth partial-tick-aware animation.
        double time = (be.getLevel().getGameTime() + partialTick) / 20.0;
        float bob = (float) Math.sin(time * Math.PI * 0.5) * 0.06f; // ~0.25 Hz, ±0.06 blocks

        // The brain model spans y=2/16..13/16 in model space (centre at y=7.5/16).
        // We want the vertical centre at the block seam (y=1.0 in world space).
        // poseStack is currently translated +1 Y, so in this space y=0 is the seam.
        // Offset: centre the model → translate by -7.5/16 in Y, plus bob.
        // Centre in X/Z: model is designed for a 1-block origin, so translate to (0.5, *, 0.5).
        TextureAtlasSprite brainSprite = sprite(BRAIN_TEXTURE);
        poseStack.translate(0, -7.5f / 16 + bob, 0);
        Matrix4f brainMat = poseStack.last().pose();
        drawBrain(solid, brainMat, brainSprite, packedLight, packedOverlay);

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

    /**
     * Draws the brain 3D model directly as BER geometry.
     * All coordinates are in block units (1/16 steps), matching the JSON model provided.
     * The model is centred at x=0.5, z=0.5 and the caller has already translated Y so
     * that y=7.5/16 (the model's vertical centre) sits at the block seam.
     */
    private static void drawBrain(VertexConsumer vc, Matrix4f mat, TextureAtlasSprite sp, int light, int overlay) {
        // Lower lobe: [4,4,4] to [12,8,12] — all 6 faces
        drawBox(vc, mat, 4f / 16, 4f / 16, 4f / 16, 12f / 16, 8f / 16, 12f / 16, sp, 255, 255, 255, light, overlay);

        // Upper rounded cap (center): [5,8,5] to [11,13,11] — no down face (hidden inside lower lobe)
        drawBoxFaces(
                vc, mat, 5f / 16, 8f / 16, 5f / 16, 11f / 16, 13f / 16, 11f / 16, sp, light, overlay, true, false, true,
                true, true, true); // up, down, north, south, west, east

        // Left rounding: [4,8,6] to [6,12,10] — north, south, west, up (no east/down — interior)
        drawBoxFaces(
                vc, mat, 4f / 16, 8f / 16, 6f / 16, 6f / 16, 12f / 16, 10f / 16, sp, light, overlay, true, false, true,
                true, true, false);

        // Right rounding: [10,8,6] to [12,12,10] — north, south, east, up (no west/down — interior)
        drawBoxFaces(
                vc, mat, 10f / 16, 8f / 16, 6f / 16, 12f / 16, 12f / 16, 10f / 16, sp, light, overlay, true, false,
                true, true, false, true);

        // Front rounding: [6,8,4] to [10,12,6] — north, east, west, up (no south/down — interior)
        drawBoxFaces(
                vc, mat, 6f / 16, 8f / 16, 4f / 16, 10f / 16, 12f / 16, 6f / 16, sp, light, overlay, true, false, true,
                false, true, true);

        // Back rounding: [6,8,10] to [10,12,12] — south, east, west, up (no north/down — interior)
        drawBoxFaces(
                vc, mat, 6f / 16, 8f / 16, 10f / 16, 10f / 16, 12f / 16, 12f / 16, sp, light, overlay, true, false,
                false, true, true, true);

        // Brain stem: [7,2,7] to [9,4,9] — north, south, east, west, down (no up — inside lower lobe)
        drawBoxFaces(
                vc, mat, 7f / 16, 2f / 16, 7f / 16, 9f / 16, 4f / 16, 9f / 16, sp, light, overlay, false, true, true,
                true, true, true);
    }

    /**
     * Draws a subset of the 6 faces of an axis-aligned box.
     * Boolean flags: up, down, north (-Z), south (+Z), west (-X), east (+X).
     */
    private static void drawBoxFaces(
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
            int overlay,
            boolean up,
            boolean down,
            boolean north,
            boolean south,
            boolean west,
            boolean east) {
        float u0v = sp.getU0(), u1v = sp.getU1(), v0v = sp.getV0(), v1v = sp.getV1();
        int r = 255, g = 255, b = 255;
        if (down)
            quad(
                    vc, mat, r, g, b, light, overlay, u0v, v0v, u1v, v1v, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0,
                    z0, 0, -1, 0);
        if (up)
            quad(
                    vc, mat, r, g, b, light, overlay, u0v, v0v, u1v, v1v, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1,
                    z1, 0, 1, 0);
        if (north)
            quad(
                    vc, mat, r, g, b, light, overlay, u0v, v0v, u1v, v1v, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0,
                    z0, 0, 0, -1);
        if (south)
            quad(
                    vc, mat, r, g, b, light, overlay, u0v, v0v, u1v, v1v, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0,
                    z1, 0, 0, 1);
        if (west)
            quad(
                    vc, mat, r, g, b, light, overlay, u0v, v0v, u1v, v1v, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0,
                    z0, -1, 0, 0);
        if (east)
            quad(
                    vc, mat, r, g, b, light, overlay, u0v, v0v, u1v, v1v, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0,
                    z1, 1, 0, 0);
    }

    /**
     * Draws only the exterior faces of the octagonal glass cylinder (the union of three
     * overlapping cross boxes). Emitting only exterior quads avoids Z-fighting and
     * double-blending on the internal overlap faces.
     *
     * <p>Cross footprint (in 1/16 units):
     * <ul>
     *   <li>Box A (centre): x=3..13, z=3..13
     *   <li>Box B (E-W arm): x=2..14, z=4..12
     *   <li>Box C (N-S arm): x=4..12, z=2..14
     * </ul>
     * y range: 4/16 (base top) to 30/16 (1 + 14/16, dome base).
     */
    private static void drawGlassCylinder(
            VertexConsumer vc, Matrix4f mat, TextureAtlasSprite sp, int light, int overlay) {
        float y0 = 4f / 16, y1 = 30f / 16;
        int r = 255, g = 255, b = 255;
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();

        // ── NORTH faces (normal -Z): v order = (xMax,y0,z),(xMin,y0,z),(xMin,y1,z),(xMax,y1,z)
        // N1: Box B north, west stub (x=2..3, z=4)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 3f / 16, y0, 4f / 16, 2f / 16, y0, 4f / 16, 2f / 16,
                y1, 4f / 16, 3f / 16, y1, 4f / 16, 0, 0, -1);
        // N2: Box A north, west stub (x=3..4, z=3)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 4f / 16, y0, 3f / 16, 3f / 16, y0, 3f / 16, 3f / 16,
                y1, 3f / 16, 4f / 16, y1, 3f / 16, 0, 0, -1);
        // N3: Box C north face (x=4..12, z=2)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 12f / 16, y0, 2f / 16, 4f / 16, y0, 2f / 16, 4f / 16,
                y1, 2f / 16, 12f / 16, y1, 2f / 16, 0, 0, -1);
        // N4: Box A north, east stub (x=12..13, z=3)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 13f / 16, y0, 3f / 16, 12f / 16, y0, 3f / 16,
                12f / 16, y1, 3f / 16, 13f / 16, y1, 3f / 16, 0, 0, -1);
        // N5: Box B north, east stub (x=13..14, z=4)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 14f / 16, y0, 4f / 16, 13f / 16, y0, 4f / 16,
                13f / 16, y1, 4f / 16, 14f / 16, y1, 4f / 16, 0, 0, -1);

        // ── SOUTH faces (normal +Z): v order = (xMin,y0,z),(xMax,y0,z),(xMax,y1,z),(xMin,y1,z)
        // S1: Box B south, west stub (x=2..3, z=12)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 2f / 16, y0, 12f / 16, 3f / 16, y0, 12f / 16, 3f / 16,
                y1, 12f / 16, 2f / 16, y1, 12f / 16, 0, 0, 1);
        // S2: Box A south, west stub (x=3..4, z=13)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 3f / 16, y0, 13f / 16, 4f / 16, y0, 13f / 16, 4f / 16,
                y1, 13f / 16, 3f / 16, y1, 13f / 16, 0, 0, 1);
        // S3: Box C south face (x=4..12, z=14)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 4f / 16, y0, 14f / 16, 12f / 16, y0, 14f / 16,
                12f / 16, y1, 14f / 16, 4f / 16, y1, 14f / 16, 0, 0, 1);
        // S4: Box A south, east stub (x=12..13, z=13)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 12f / 16, y0, 13f / 16, 13f / 16, y0, 13f / 16,
                13f / 16, y1, 13f / 16, 12f / 16, y1, 13f / 16, 0, 0, 1);
        // S5: Box B south, east stub (x=13..14, z=12)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 13f / 16, y0, 12f / 16, 14f / 16, y0, 12f / 16,
                14f / 16, y1, 12f / 16, 13f / 16, y1, 12f / 16, 0, 0, 1);

        // ── WEST faces (normal -X): v order = (x,y0,zMin),(x,y0,zMax),(x,y1,zMax),(x,y1,zMin)
        // W1: Box C west, north stub (z=2..3, x=4)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 4f / 16, y0, 2f / 16, 4f / 16, y0, 3f / 16, 4f / 16,
                y1, 3f / 16, 4f / 16, y1, 2f / 16, -1, 0, 0);
        // W2: Box A west, north stub (z=3..4, x=3)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 3f / 16, y0, 3f / 16, 3f / 16, y0, 4f / 16, 3f / 16,
                y1, 4f / 16, 3f / 16, y1, 3f / 16, -1, 0, 0);
        // W3: Box B west face (z=4..12, x=2)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 2f / 16, y0, 4f / 16, 2f / 16, y0, 12f / 16, 2f / 16,
                y1, 12f / 16, 2f / 16, y1, 4f / 16, -1, 0, 0);
        // W4: Box A west, south stub (z=12..13, x=3)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 3f / 16, y0, 12f / 16, 3f / 16, y0, 13f / 16, 3f / 16,
                y1, 13f / 16, 3f / 16, y1, 12f / 16, -1, 0, 0);
        // W5: Box C west, south stub (z=13..14, x=4)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 4f / 16, y0, 13f / 16, 4f / 16, y0, 14f / 16, 4f / 16,
                y1, 14f / 16, 4f / 16, y1, 13f / 16, -1, 0, 0);

        // ── EAST faces (normal +X): v order = (x,y0,zMax),(x,y0,zMin),(x,y1,zMin),(x,y1,zMax)
        // E1: Box C east, north stub (z=2..3, x=12)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 12f / 16, y0, 3f / 16, 12f / 16, y0, 2f / 16,
                12f / 16, y1, 2f / 16, 12f / 16, y1, 3f / 16, 1, 0, 0);
        // E2: Box A east, north stub (z=3..4, x=13)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 13f / 16, y0, 4f / 16, 13f / 16, y0, 3f / 16,
                13f / 16, y1, 3f / 16, 13f / 16, y1, 4f / 16, 1, 0, 0);
        // E3: Box B east face (z=4..12, x=14)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 14f / 16, y0, 12f / 16, 14f / 16, y0, 4f / 16,
                14f / 16, y1, 4f / 16, 14f / 16, y1, 12f / 16, 1, 0, 0);
        // E4: Box A east, south stub (z=12..13, x=13)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 13f / 16, y0, 13f / 16, 13f / 16, y0, 12f / 16,
                13f / 16, y1, 12f / 16, 13f / 16, y1, 13f / 16, 1, 0, 0);
        // E5: Box C east, south stub (z=13..14, x=12)
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 12f / 16, y0, 14f / 16, 12f / 16, y0, 13f / 16,
                12f / 16, y1, 13f / 16, 12f / 16, y1, 14f / 16, 1, 0, 0);

        // ── BOTTOM cap (normal -Y): the octagonal footprint at y=4/16
        // Five quads tessellating the octagon, winding CCW from below
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 3f / 16, y0, 4f / 16, 2f / 16, y0, 4f / 16, 2f / 16,
                y0, 12f / 16, 3f / 16, y0, 12f / 16, 0, -1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 4f / 16, y0, 3f / 16, 3f / 16, y0, 3f / 16, 3f / 16,
                y0, 13f / 16, 4f / 16, y0, 13f / 16, 0, -1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 12f / 16, y0, 2f / 16, 4f / 16, y0, 2f / 16, 4f / 16,
                y0, 14f / 16, 12f / 16, y0, 14f / 16, 0, -1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 13f / 16, y0, 3f / 16, 12f / 16, y0, 3f / 16,
                12f / 16, y0, 13f / 16, 13f / 16, y0, 13f / 16, 0, -1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 14f / 16, y0, 4f / 16, 13f / 16, y0, 4f / 16,
                13f / 16, y0, 12f / 16, 14f / 16, y0, 12f / 16, 0, -1, 0);

        // ── TOP cap (normal +Y): the octagonal footprint at y=30/16
        // Five quads tessellating the octagon, winding CCW from above
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 2f / 16, y1, 4f / 16, 3f / 16, y1, 4f / 16, 3f / 16,
                y1, 12f / 16, 2f / 16, y1, 12f / 16, 0, 1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 3f / 16, y1, 3f / 16, 4f / 16, y1, 3f / 16, 4f / 16,
                y1, 13f / 16, 3f / 16, y1, 13f / 16, 0, 1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 4f / 16, y1, 2f / 16, 12f / 16, y1, 2f / 16, 12f / 16,
                y1, 14f / 16, 4f / 16, y1, 14f / 16, 0, 1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 12f / 16, y1, 3f / 16, 13f / 16, y1, 3f / 16,
                13f / 16, y1, 13f / 16, 12f / 16, y1, 13f / 16, 0, 1, 0);
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, 13f / 16, y1, 4f / 16, 14f / 16, y1, 4f / 16,
                14f / 16, y1, 12f / 16, 13f / 16, y1, 12f / 16, 0, 1, 0);
    }

    /** Draws a flat quad for a status dot (solid colour, no UV mapping needed). */
    private static void dot(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int light,
            int overlay,
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
        float u = 0.5f, v = 0.5f; // arbitrary UV — colour comes from vertex colour
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
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
