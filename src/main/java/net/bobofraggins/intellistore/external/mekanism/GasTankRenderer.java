package net.bobofraggins.intellistore.external.mekanism;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.api.chemical.ChemicalStack;
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
 * Renders the Gas Tank as a glass cylinder with a variable chemical fill level.
 *
 * <p>Geometry and texture are identical to {@link net.bobofraggins.intellistore.storage.fluidtank.FluidTankRenderer}
 * — metal rims, then chemical fill (translucent, coloured by {@link ChemicalStack#getChemicalTint()}),
 * then glass walls.
 */
public class GasTankRenderer implements BlockEntityRenderer<GasTankBlockEntity> {

    // -------------------------------------------------------------------------
    // Texture locations (reuse Fluid Tank textures)
    // -------------------------------------------------------------------------

    private static final ResourceLocation TANK_BODY =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/fluid_tank");
    private static final ResourceLocation JAR_GLASS =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/jar_glass");

    // -------------------------------------------------------------------------
    // Geometry constants — identical to FluidTankRenderer
    // -------------------------------------------------------------------------

    private static final float RIM_H = 2f / 16f;

    private static final float BODY_A0 = 1f / 16f;
    private static final float BODY_A1 = 15f / 16f;
    private static final float BODY_B0 = 2f / 16f;
    private static final float BODY_B1 = 14f / 16f;

    private static final float GLASS_A0 = 2f / 16f;
    private static final float GLASS_A1 = 14f / 16f;
    private static final float GLASS_B0 = 3f / 16f;
    private static final float GLASS_B1 = 13f / 16f;
    private static final float GLASS_FLOOR = RIM_H;
    private static final float GLASS_CEIL = 1f - RIM_H;

    private static final float FLUID_A0 = 3f / 16f;
    private static final float FLUID_A1 = 13f / 16f;
    private static final float FLUID_B0 = 4f / 16f;
    private static final float FLUID_B1 = 12f / 16f;
    private static final float FLUID_FLOOR = GLASS_FLOOR + 1f / 16f;
    private static final float FLUID_CEIL = GLASS_CEIL - 1f / 16f;
    private static final float FLUID_INTERIOR_H = FLUID_CEIL - FLUID_FLOOR;

    private static final float MIN_FILL_FRAC = 0.05f;

    public GasTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(
            GasTankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        TextureAtlasSprite bodySprite = sprite(TANK_BODY);
        TextureAtlasSprite glassSprite = sprite(JAR_GLASS);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
        VertexConsumer translucent = bufferSource.getBuffer(RenderType.translucent());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Metal body — bottom rim ----
        drawCylinder(
                solid,
                mat,
                BODY_A0,
                0,
                BODY_A1,
                RIM_H,
                BODY_B0,
                BODY_B1,
                bodySprite,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        // ---- Metal body — top rim ----
        drawCylinder(
                solid,
                mat,
                BODY_A0,
                1f - RIM_H,
                BODY_A1,
                1f,
                BODY_B0,
                BODY_B1,
                bodySprite,
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
            float fillTop = FLUID_FLOOR + fillFrac * FLUID_INTERIOR_H;

            // getChemicalTint() returns ARGB int (same format as fluid tint)
            int tint = chemical.getChemicalTint();
            int fr = (tint >> 16) & 0xFF;
            int fg = (tint >> 8) & 0xFF;
            int fb = tint & 0xFF;
            int fa = (tint >> 24) & 0xFF;
            if (fa == 0) fa = 255;

            // Use the glass texture as a stand-in for the gas fill (semi-transparent tinted solid)
            drawCylinder(
                    translucent,
                    mat,
                    FLUID_A0,
                    FLUID_FLOOR,
                    FLUID_A1,
                    fillTop,
                    FLUID_B0,
                    FLUID_B1,
                    glassSprite,
                    fr,
                    fg,
                    fb,
                    fa,
                    packedLight,
                    packedOverlay);
        }

        // ---- Glass cylinder (translucent) — drawn after fill ----
        drawCylinder(
                translucent,
                mat,
                GLASS_A0,
                GLASS_FLOOR,
                GLASS_A1,
                GLASS_CEIL,
                GLASS_B0,
                GLASS_B1,
                glassSprite,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        poseStack.popPose();
    }

    // -------------------------------------------------------------------------
    // Geometry helpers (identical to FluidTankRenderer)
    // -------------------------------------------------------------------------

    private static TextureAtlasSprite sprite(ResourceLocation loc) {
        return Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(loc);
    }

    private static void drawCylinder(
            VertexConsumer vc,
            Matrix4f mat,
            float xA0,
            float y0,
            float xA1,
            float y1,
            float zA0,
            float zA1,
            TextureAtlasSprite sp,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay) {
        drawBox(vc, mat, xA0, y0, zA0, xA1, y1, zA1, sp, r, g, b, a, light, overlay);
        drawBox(vc, mat, zA0, y0, xA0, zA1, y1, xA1, sp, r, g, b, a, light, overlay);
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
