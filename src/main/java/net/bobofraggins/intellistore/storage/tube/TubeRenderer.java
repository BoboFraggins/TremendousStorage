package net.bobofraggins.intellistore.storage.tube;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.intellistore.storage.tubeattachments.AttachmentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.joml.Matrix4f;

/**
 * Renders the dynamic geometry of a Tube: core cube, arms toward connected faces,
 * and Storage Interface attachment plates. All geometry is tinted by the tube's dye color.
 *
 * <p>The JSON block model provides only the particle texture (for break effects). All
 * visible geometry is drawn here.
 *
 * <p>Face shading multipliers give each face a distinct tone so tubes read clearly:
 * <ul>
 *   <li>Up/Down faces — 90% brightness (darker top/bottom)
 *   <li>East/West faces — 95% brightness (slightly dimmed sides)
 *   <li>North/South faces — 100% brightness (pure dye color)
 * </ul>
 */
public class TubeRenderer implements BlockEntityRenderer<TubeBlockEntity> {

    /** ResourceLocation for the white tube texture (tinted by DyeColor). */
    public static final ResourceLocation TUBE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/tube");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    // Block-unit coordinate constants (in [0..1] space)
    private static final float C_MIN = 6f / 16f; // Core start
    private static final float C_MAX = 10f / 16f; // Core end
    private static final float A_MIN = C_MIN; // Arm cross-section min (same as core)
    private static final float A_MAX = C_MAX; // Arm cross-section max
    // Arms stop just short of the block face to avoid Z-fighting with the neighbour's face
    private static final float A_FACE_LO = EPS;
    private static final float A_FACE_HI = 1f - EPS;

    // Attachment plate is 8/16 wide, 2/16 thick
    private static final float P_MIN = 4f / 16f;
    private static final float P_MAX = 12f / 16f;
    private static final float P_THICK = 2f / 16f;

    // Per-face brightness multipliers
    private static final float SHADE_TOP_BOTTOM = 0.90f;
    private static final float SHADE_EAST_WEST = 0.95f;
    private static final float SHADE_NORTH_SOUTH = 1.00f;

    public TubeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            TubeBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof TubeBlock tubeBlock)) return;

        DyeColor dye = tubeBlock.getColor();
        int texColor = dye.getTextureDiffuseColor();
        int r = (texColor >> 16) & 0xFF;
        int g = (texColor >> 8) & 0xFF;
        int b = texColor & 0xFF;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(TUBE_TEXTURE);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.cutout());
        poseStack.pushPose();

        Matrix4f mat = poseStack.last().pose();

        // Core cube
        drawBox(vc, mat, C_MIN, C_MIN, C_MIN, C_MAX, C_MAX, C_MAX, sprite, r, g, b, packedLight, packedOverlay);

        // Arms toward connected faces
        for (Direction dir : Direction.values()) {
            if (state.getValue(dirProp(dir))) {
                drawArm(vc, mat, dir, sprite, r, g, b, packedLight, packedOverlay);
            }
        }

        // Attachment plates — color varies by attachment type
        for (int i = 0; i < 6; i++) {
            AttachmentType aType = be.getAttachmentType(i);
            if (aType == AttachmentType.NONE) continue;
            int pr, pg, pb;
            switch (aType) {
                case IMPORT_INTERFACE -> {
                    pr = 0x33;
                    pg = 0x99;
                    pb = 0xFF;
                } // blue
                case EXPORT_INTERFACE -> {
                    pr = 0xFF;
                    pg = 0x33;
                    pb = 0x33;
                } // red
                case PLACER_INTERFACE -> {
                    pr = 0x33;
                    pg = 0xFF;
                    pb = 0x33;
                } // green
                case BREAKER_INTERFACE -> {
                    pr = 0xFF;
                    pg = 0xFF;
                    pb = 0x00;
                } // yellow
                default -> {
                    pr = r;
                    pg = g;
                    pb = b;
                } // tube color (Storage Interface)
            }
            drawAttachmentPlate(vc, mat, Direction.values()[i], sprite, pr, pg, pb, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static BooleanProperty dirProp(Direction dir) {
        return switch (dir) {
            case DOWN -> TubeBlock.DOWN;
            case UP -> TubeBlock.UP;
            case NORTH -> TubeBlock.NORTH;
            case SOUTH -> TubeBlock.SOUTH;
            case WEST -> TubeBlock.WEST;
            case EAST -> TubeBlock.EAST;
        };
    }

    /** Draws a 4×4 arm from the core out to the given face. */
    private static void drawArm(
            VertexConsumer vc,
            Matrix4f mat,
            Direction dir,
            TextureAtlasSprite sprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case DOWN -> {
                x0 = A_MIN;
                y0 = A_FACE_LO;
                z0 = A_MIN;
                x1 = A_MAX;
                y1 = C_MIN;
                z1 = A_MAX;
            }
            case UP -> {
                x0 = A_MIN;
                y0 = C_MAX;
                z0 = A_MIN;
                x1 = A_MAX;
                y1 = A_FACE_HI;
                z1 = A_MAX;
            }
            case NORTH -> {
                x0 = A_MIN;
                y0 = A_MIN;
                z0 = A_FACE_LO;
                x1 = A_MAX;
                y1 = A_MAX;
                z1 = C_MIN;
            }
            case SOUTH -> {
                x0 = A_MIN;
                y0 = A_MIN;
                z0 = C_MAX;
                x1 = A_MAX;
                y1 = A_MAX;
                z1 = A_FACE_HI;
            }
            case WEST -> {
                x0 = A_FACE_LO;
                y0 = A_MIN;
                z0 = A_MIN;
                x1 = C_MIN;
                y1 = A_MAX;
                z1 = A_MAX;
            }
            default -> {
                x0 = C_MAX;
                y0 = A_MIN;
                z0 = A_MIN;
                x1 = A_FACE_HI;
                y1 = A_MAX;
                z1 = A_MAX;
            } // EAST
        }
        drawBox(vc, mat, x0, y0, z0, x1, y1, z1, sprite, r, g, b, light, overlay);
    }

    /**
     * Draws an 8×8×2 attachment plate flush with the outside face, slightly inset to avoid
     * z-fighting with adjacent blocks.
     */
    private static void drawAttachmentPlate(
            VertexConsumer vc,
            Matrix4f mat,
            Direction dir,
            TextureAtlasSprite sprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        float inset = EPS;
        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case DOWN -> {
                x0 = P_MIN;
                y0 = inset;
                z0 = P_MIN;
                x1 = P_MAX;
                y1 = P_THICK + inset;
                z1 = P_MAX;
            }
            case UP -> {
                x0 = P_MIN;
                y0 = 1 - P_THICK - inset;
                z0 = P_MIN;
                x1 = P_MAX;
                y1 = 1 - inset;
                z1 = P_MAX;
            }
            case NORTH -> {
                x0 = P_MIN;
                y0 = P_MIN;
                z0 = inset;
                x1 = P_MAX;
                y1 = P_MAX;
                z1 = P_THICK + inset;
            }
            case SOUTH -> {
                x0 = P_MIN;
                y0 = P_MIN;
                z0 = 1 - P_THICK - inset;
                x1 = P_MAX;
                y1 = P_MAX;
                z1 = 1 - inset;
            }
            case WEST -> {
                x0 = inset;
                y0 = P_MIN;
                z0 = P_MIN;
                x1 = P_THICK + inset;
                y1 = P_MAX;
                z1 = P_MAX;
            }
            default -> {
                x0 = 1 - P_THICK - inset;
                y0 = P_MIN;
                z0 = P_MIN;
                x1 = 1 - inset;
                y1 = P_MAX;
                z1 = P_MAX;
            } // EAST
        }
        drawBox(vc, mat, x0, y0, z0, x1, y1, z1, sprite, r, g, b, light, overlay);
    }

    /**
     * Draws all 6 faces of an axis-aligned box with per-face shading.
     * Each face receives a flat brightness multiplier based on its orientation:
     * Up/Down=90%, East/West=95%, North/South=100%.
     */
    private static void drawBox(
            VertexConsumer vc,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            TextureAtlasSprite sprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        int ry = shade(r, SHADE_TOP_BOTTOM), gy = shade(g, SHADE_TOP_BOTTOM), by = shade(b, SHADE_TOP_BOTTOM);
        int rx = shade(r, SHADE_EAST_WEST), gx = shade(g, SHADE_EAST_WEST), bx = shade(b, SHADE_EAST_WEST);
        // North/South: pure color (no change needed, use r/g/b directly)

        // -Y (down) — top/bottom shade
        quad(
                vc, mat, ry, gy, by, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0,
                -1, 0);
        // +Y (up) — top/bottom shade
        quad(
                vc, mat, ry, gy, by, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0,
                1, 0);
        // -Z (north) — pure color
        quad(
                vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0,
                -1);
        // +Z (south) — pure color
        quad(vc, mat, r, g, b, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1);
        // -X (west) — east/west shade
        quad(
                vc, mat, rx, gx, bx, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1,
                0, 0);
        // +X (east) — east/west shade
        quad(
                vc, mat, rx, gx, bx, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1,
                0, 0);
    }

    /** Multiplies a channel value by a shade factor and clamps to [0, 255]. */
    private static int shade(int channel, float factor) {
        return Math.min(255, Math.max(0, Math.round(channel * factor)));
    }

    /** Emits a single CCW quad (4 vertices) with the given UV corners. */
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
