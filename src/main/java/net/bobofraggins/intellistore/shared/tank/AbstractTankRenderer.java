package net.bobofraggins.intellistore.shared.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

/**
 * Shared BESR base for all tank types (fluid, gas, source).
 *
 * <p>Handles tube-connector stub rendering and the shared octagonal-prism geometry helpers.
 * Subclasses implement {@link #renderFill} to paint their specific fill material.
 */
public abstract class AbstractTankRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    protected static final ResourceLocation LAZURITE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/lazurite_block");

    // Fluid/fill interior bounds (inside the glass jar)
    protected static final float FLUID_FLOOR = 3f / 16f;
    protected static final float FLUID_CEIL = 13f / 16f;
    protected static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    // Tube connector stub dimensions
    private static final float STUB_D = 2f / 16f;
    private static final float STUB_MIN = 6f / 16f;
    private static final float STUB_MAX = 10f / 16f;

    // Octagon XZ vertices (all in [0,1] block space)
    // A = NW, B = NE, C = EN, D = ES, E = SE, F = SW, G = WS, H = WN
    protected static final float AX = 5.5f / 16f, AZ = 2f / 16f;
    protected static final float BX = 10.5f / 16f, BZ = 2f / 16f;
    protected static final float CX = 14f / 16f, CZ = 5.5f / 16f;
    protected static final float DX = 14f / 16f, DZ = 10.5f / 16f;
    protected static final float EX = 10.5f / 16f, EZ = 14f / 16f;
    protected static final float FX = 5.5f / 16f, FZ = 14f / 16f;
    protected static final float GX = 2f / 16f, GZ = 10.5f / 16f;
    protected static final float HX = 2f / 16f, HZ = 5.5f / 16f;

    /**
     * Renders the fill material for this tank type.
     * Called once per frame if the block entity has content to show.
     */
    protected abstract void renderFill(
            T be, Matrix4f mat, MultiBufferSource bufferSource, int packedLight, int packedOverlay);

    @Override
    public void render(
            T be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        renderFill(be, mat, bufferSource, packedLight, packedOverlay);

        // ---- Tube connector stubs (solid lazurite) ----
        Level level = be.getLevel();
        if (level != null) {
            BlockPos tankPos = be.getBlockPos();
            boolean hasStub = false;
            for (Direction dir : Direction.values()) {
                if (level.getBlockState(tankPos.relative(dir)).getBlock() instanceof TubeBlock) {
                    hasStub = true;
                    break;
                }
            }
            if (hasStub) {
                TextureAtlasSprite lazuriteSprite = sprite(LAZURITE_BLOCK);
                VertexConsumer solid = bufferSource.getBuffer(Sheets.solidBlockSheet());
                for (Direction dir : Direction.values()) {
                    if (level.getBlockState(tankPos.relative(dir)).getBlock() instanceof TubeBlock) {
                        drawStub(solid, mat, dir, lazuriteSprite, packedLight, packedOverlay);
                    }
                }
            }
        }

        poseStack.popPose();
    }

    // -------------------------------------------------------------------------
    // Geometry helpers (available to subclasses)
    // -------------------------------------------------------------------------

    protected static TextureAtlasSprite sprite(ResourceLocation loc) {
        return Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(loc);
    }

    private static void drawStub(
            VertexConsumer vc, Matrix4f mat, Direction dir, TextureAtlasSprite sp, int light, int overlay) {
        float s = STUB_MIN, e = STUB_MAX, d = STUB_D;
        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case DOWN -> {
                x0 = s;
                y0 = 0;
                z0 = s;
                x1 = e;
                y1 = d;
                z1 = e;
            }
            case UP -> {
                x0 = s;
                y0 = 1 - d;
                z0 = s;
                x1 = e;
                y1 = 1;
                z1 = e;
            }
            case NORTH -> {
                x0 = s;
                y0 = s;
                z0 = 0;
                x1 = e;
                y1 = e;
                z1 = d;
            }
            case SOUTH -> {
                x0 = s;
                y0 = s;
                z0 = 1 - d;
                x1 = e;
                y1 = e;
                z1 = 1;
            }
            case WEST -> {
                x0 = 0;
                y0 = s;
                z0 = s;
                x1 = d;
                y1 = e;
                z1 = e;
            }
            default -> {
                x0 = 1 - d;
                y0 = s;
                z0 = s;
                x1 = 1;
                y1 = e;
                z1 = e;
            }
        }
        drawBox(vc, mat, x0, y0, z0, x1, y1, z1, sp, 255, 255, 255, 255, light, overlay, dir);
    }

    protected static void drawBox(
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
            int a,
            int light,
            int overlay,
            Direction skipFace) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        if (skipFace != Direction.DOWN)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                    0, -1, 0);
        if (skipFace != Direction.UP)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                    0, 1, 0);
        if (skipFace != Direction.NORTH)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0,
                    0, 0, -1);
        if (skipFace != Direction.SOUTH)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1,
                    0, 0, 1);
        if (skipFace != Direction.WEST)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0,
                    -1, 0, 0);
        if (skipFace != Direction.EAST)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1,
                    1, 0, 0);
    }

    /**
     * Emits a single quad using shared left/right UV and per-vertex top/bottom V.
     * Vertex order: v0, v1, v2, v3 (CCW from outside).
     */
    @SuppressWarnings("java:S107")
    protected static void quadFluid(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay,
            float uLeft,
            float vTop,
            float uRight,
            float vBottom,
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
                .setColor(r, g, b, a)
                .setUv(uLeft, vBottom)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, a)
                .setUv(uRight, vBottom)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, a)
                .setUv(uRight, vTop)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, a)
                .setUv(uLeft, vTop)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    /** Emits a single quad with explicit per-vertex atlas UV coordinates. */
    @SuppressWarnings("java:S107")
    protected static void quadFluidV(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay,
            float x0,
            float y0,
            float z0,
            float su0,
            float sv0,
            float x1,
            float y1,
            float z1,
            float su1,
            float sv1,
            float x2,
            float y2,
            float z2,
            float su2,
            float sv2,
            float x3,
            float y3,
            float z3,
            float su3,
            float sv3,
            float nx,
            float ny,
            float nz) {
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, a)
                .setUv(su0, sv0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, a)
                .setUv(su1, sv1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, a)
                .setUv(su2, sv2)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, a)
                .setUv(su3, sv3)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    @SuppressWarnings("java:S107")
    protected static void quad(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int a,
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
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, a)
                .setUv(u1, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, a)
                .setUv(u0, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
