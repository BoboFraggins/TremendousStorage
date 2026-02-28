package net.bobofraggins.intellistore.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
        drawBox(
                solid,
                mat,
                3f / 16,
                EPS,
                3f / 16,
                13f / 16,
                4f / 16,
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

        // ---- Water interior — translucent, inset inside glass ----
        // Lower half: from top of base to top of lower block
        drawBox(
                translucent,
                mat,
                4f / 16,
                5f / 16,
                4f / 16,
                12f / 16,
                1f,
                12f / 16,
                waterSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Glass cylinder — translucent, three overlapping boxes ----
        // Extends from top of iron base (4/16) to top of lower block (1.0) so there is no gap.
        drawBox(
                translucent,
                mat,
                3f / 16,
                4f / 16,
                3f / 16,
                13f / 16,
                1f,
                13f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawBox(
                translucent,
                mat,
                2f / 16,
                4f / 16,
                4f / 16,
                14f / 16,
                1f,
                12f / 16,
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
                4f / 16,
                2f / 16,
                12f / 16,
                1f,
                14f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

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

        // Water interior continuation — upper block, inset inside glass, up to dome base.
        // Start at -EPS to overlap the lower/upper block seam and prevent Z-fighting there.
        drawBox(
                translucent,
                mat,
                4f / 16,
                -EPS,
                4f / 16,
                12f / 16,
                14f / 16,
                12f / 16,
                waterSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // Glass cylinder continuation — fills upper block to y=14/16 where dome begins.
        // Start at -EPS to overlap the lower/upper block seam and prevent Z-fighting there.
        drawBox(
                translucent,
                mat,
                3f / 16,
                -EPS,
                3f / 16,
                13f / 16,
                14f / 16,
                13f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawBox(
                translucent,
                mat,
                2f / 16,
                -EPS,
                4f / 16,
                14f / 16,
                14f / 16,
                12f / 16,
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
                -EPS,
                2f / 16,
                12f / 16,
                14f / 16,
                14f / 16,
                glassSprite,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

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

        // ---- Animated floating brain ----
        // Use game time for smooth partial-tick-aware animation (avoids System.currentTimeMillis stutter)
        double time = (be.getLevel().getGameTime() + partialTick) / 20.0;
        float bob = (float) Math.sin(time * Math.PI * 0.5) * 0.06f; // ~0.25 Hz, ±0.06 blocks

        // Brain sits ~0.55 above lower-half floor.
        // poseStack is currently translated +1 Y, so subtract 1 to get back to jar-interior coords:
        // target y from lower floor: 0.55 → from +1Y offset: 0.55 - 1 = -0.45
        poseStack.translate(0.5, -0.45 + bob, 0.5);
        poseStack.scale(0.6f, 0.6f, 0.6f);

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        new ItemStack(Registration.BRAIN.get()),
                        ItemDisplayContext.GROUND,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        bufferSource,
                        be.getLevel(),
                        0);

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
