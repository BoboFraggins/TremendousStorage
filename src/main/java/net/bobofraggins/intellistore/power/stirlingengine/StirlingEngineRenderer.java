package net.bobofraggins.intellistore.power.stirlingengine;

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
 * Renders the Stirling Engine as an iron base + iron block top with a sliding copper piston.
 *
 * <p>The piston animates (bouncing up and down) only when a heat source is present below
 * the engine (i.e. when {@link StirlingEngineBlockEntity#isHeated()} returns true).
 * When idle the piston sits at its lowest position, 1 px proud of the iron block top.
 *
 * <p>Geometry (all in 1/16ths of a block):
 * <ul>
 *   <li>Iron base   — [2,EPS,2] → [14,6,14]  (12px wide, 6px tall)
 *   <li>Iron top    — [4,6,4]   → [12,10,12]  (8px wide, 4px tall, centred)
 *   <li>Copper piston — [5,?,5] → [11,?,11]  (6px wide, 6px tall, Y position animated)
 *       piston top: 11/16 (rest) … 16/16 (extended)
 * </ul>
 */
public class StirlingEngineRenderer implements BlockEntityRenderer<StirlingEngineBlockEntity> {

    private static final ResourceLocation IRON_BLOCK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/iron_block");
    private static final ResourceLocation COPPER_BLOCK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/copper_block");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    /** Piston height in world units (6 px). */
    private static final float PISTON_HEIGHT = 6f / 16f;

    /** Y of the iron-top surface (10 px). */
    private static final float IRON_TOP_Y = 10f / 16f;

    /** Piston top at rest: 1 px proud of the iron block top. */
    private static final float PISTON_TOP_MIN = IRON_TOP_Y + 1f / 16f;

    /** Piston top at full extension: top of block. */
    private static final float PISTON_TOP_MAX = 1f;

    /** animationTicks units per second (1.5 per client tick × 20 ticks/s = 30). */
    private static final float TICKS_PER_SECOND = 30f;

    /** One full piston cycle = 2 seconds. */
    private static final float CYCLE_TICKS = TICKS_PER_SECOND * 2f;

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
        TextureAtlasSprite copperSprite = sprite(COPPER_BLOCK);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Iron base: 12 px wide, 6 px tall ----
        drawBox(solid, mat, 2f / 16, EPS, 2f / 16, 14f / 16, 6f / 16, 14f / 16, ironSprite, packedLight, packedOverlay);

        // ---- Iron block top: 8 px wide, 4 px tall, centred ----
        drawBox(
                solid,
                mat,
                4f / 16,
                6f / 16,
                4f / 16,
                12f / 16,
                IRON_TOP_Y,
                12f / 16,
                ironSprite,
                packedLight,
                packedOverlay);

        // ---- Copper piston: 6 px wide, slides up/down when heated ----
        float pistonTop;
        if (be.isHeated()) {
            float time = be.animationTicks + partialTick * 1.5f;
            // sin oscillates -1 … +1; map to 0 … 1 for piston travel.
            float t = ((float) Math.sin(time * Math.PI / (CYCLE_TICKS / 2f)) + 1f) / 2f;
            pistonTop = PISTON_TOP_MIN + t * (PISTON_TOP_MAX - PISTON_TOP_MIN);
        } else {
            pistonTop = PISTON_TOP_MIN;
        }
        float pistonBot = pistonTop - PISTON_HEIGHT;

        drawBox(
                solid,
                mat,
                5f / 16,
                pistonBot,
                5f / 16,
                11f / 16,
                pistonTop,
                11f / 16,
                copperSprite,
                packedLight,
                packedOverlay);

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
