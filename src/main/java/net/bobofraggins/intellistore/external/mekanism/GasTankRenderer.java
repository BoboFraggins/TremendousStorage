package net.bobofraggins.intellistore.external.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.api.chemical.ChemicalStack;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Renders the Gas Tank as a jar with a tinted chemical fill block.
 *
 * <p>Geometry matches {@link net.bobofraggins.intellistore.storage.fluidtank.FluidTankRenderer}:
 * lazurite base slab, translucent chemical fill block, glass walls + top.
 */
public class GasTankRenderer implements BlockEntityRenderer<GasTankBlockEntity> {

    private static final ResourceLocation LAZURITE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/lazurite_block");
    private static final ResourceLocation JAR_GLASS =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/jar_glass");
    private static final ResourceLocation FLUID_TANK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/fluid_tank");

    private static final float EPS = 1e-4f;

    private static final float FLUID_MIN = 2f / 16f;
    private static final float FLUID_MAX = 14f / 16f;
    private static final float FLUID_FLOOR = 3f / 16f;
    private static final float FLUID_CEIL = 15f / 16f;
    private static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    private static final float MIN_FILL_FRAC = 0.05f;

    private static final float STUB_D = 2f / 16f;
    private static final float STUB_MIN = 6f / 16f;
    private static final float STUB_MAX = 10f / 16f;

    public GasTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            GasTankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        TextureAtlasSprite lazuriteSprite = sprite(LAZURITE_BLOCK);
        TextureAtlasSprite glassSprite = sprite(JAR_GLASS);
        TextureAtlasSprite fluidSprite = sprite(FLUID_TANK);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
        VertexConsumer translucent = bufferSource.getBuffer(RenderType.translucent());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Lazurite base (solid) ----
        drawBox(
                solid,
                mat,
                EPS,
                EPS,
                EPS,
                1f - EPS,
                2f / 16f,
                1f - EPS,
                lazuriteSprite,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Chemical fill (translucent) ----
        if (be.isLocked()) {
            ChemicalStack chemical = be.getStoredChemical();

            float fillFrac = be.getAmount() > 0
                    ? Math.max(MIN_FILL_FRAC, (float) be.getAmount() / GasTankBlockEntity.CAPACITY)
                    : MIN_FILL_FRAC;
            float fillTop = FLUID_FLOOR + fillFrac * FLUID_H;

            int tint = chemical.getChemicalTint();
            int fr = (tint >> 16) & 0xFF;
            int fg = (tint >> 8) & 0xFF;
            int fb = tint & 0xFF;
            int fa = (tint >> 24) & 0xFF;
            if (fa == 0) fa = 255;

            drawBox(
                    translucent,
                    mat,
                    FLUID_MIN,
                    FLUID_FLOOR,
                    FLUID_MIN,
                    FLUID_MAX,
                    fillTop,
                    FLUID_MAX,
                    fluidSprite,
                    fr,
                    fg,
                    fb,
                    fa,
                    packedLight,
                    packedOverlay);
        }

        // ---- Glass walls + top (translucent, no bottom, double-sided) ----
        drawBoxNoBottom(
                translucent,
                mat,
                1f / 16f,
                2f / 16f,
                1f / 16f,
                15f / 16f,
                1f,
                15f / 16f,
                glassSprite,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // ---- Tube connector stubs (solid lazurite) ----
        Level level = be.getLevel();
        if (level != null) {
            BlockPos tankPos = be.getBlockPos();
            for (Direction dir : Direction.values()) {
                if (level.getBlockState(tankPos.relative(dir)).getBlock() instanceof TubeBlock) {
                    drawStub(solid, mat, dir, lazuriteSprite, packedLight, packedOverlay);
                }
            }
        }

        poseStack.popPose();
    }

    private static TextureAtlasSprite sprite(ResourceLocation loc) {
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
        drawBox(vc, mat, x0, y0, z0, x1, y1, z1, sp, 255, 255, 255, 255, light, overlay);
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
            int a,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0,
                -1, 0);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0,
                1, 0);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0,
                0, -1);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0,
                0, 1);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1,
                0, 0);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1,
                0, 0);
    }

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
            int a,
            int light,
            int overlay) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        // +Y outer then inner
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0,
                1, 0);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0,
                -1, 0);
        // -Z (north) outer then inner
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0,
                0, -1);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0,
                0, 1);
        // +Z (south) outer then inner
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0,
                0, 1);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0,
                0, -1);
        // -X (west) outer then inner
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1,
                0, 0);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, 1,
                0, 0);
        // +X (east) outer then inner
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1,
                0, 0);
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, -1,
                0, 0);
    }

    @SuppressWarnings("java:S107")
    private static void quad(
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
