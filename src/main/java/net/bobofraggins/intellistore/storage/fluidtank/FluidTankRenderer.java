package net.bobofraggins.intellistore.storage.fluidtank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the Fluid Tank's dynamic content: fluid fill level and tube-connector stubs.
 *
 * <p>The static shell (lazurite base slab and glass jar walls) is rendered by the block model
 * ({@code models/block/fluid_tank.json}) via {@link net.minecraft.world.level.block.RenderShape#MODEL}.
 * This BESR is responsible only for the parts that change at runtime:
 * <ul>
 *   <li>Fluid fill octagonal prism (translucent, height proportional to fill fraction)
 *   <li>Tube-connector stubs on faces adjacent to a {@link TubeBlock} (solid lazurite)
 * </ul>
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {

    private static final ResourceLocation LAZURITE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/lazurite_block");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    // Fluid interior bounds (inside the glass jar)
    private static final float FLUID_FLOOR = 3f / 16f;
    private static final float FLUID_CEIL = 13f / 16f;
    private static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    // Tube connector stub dimensions
    private static final float STUB_D = 2f / 16f;
    private static final float STUB_MIN = 6f / 16f;
    private static final float STUB_MAX = 10f / 16f;

    // Octagon XZ vertices (all in [0,1] block space)
    // A = NW, B = NE, C = EN, D = ES, E = SE, F = SW, G = WS, H = WN
    private static final float AX = 5.5f / 16f, AZ = 2f / 16f;
    private static final float BX = 10.5f / 16f, BZ = 2f / 16f;
    private static final float CX = 14f / 16f, CZ = 5.5f / 16f;
    private static final float DX = 14f / 16f, DZ = 10.5f / 16f;
    private static final float EX = 10.5f / 16f, EZ = 14f / 16f;
    private static final float FX = 5.5f / 16f, FZ = 14f / 16f;
    private static final float GX = 2f / 16f, GZ = 10.5f / 16f;
    private static final float HX = 2f / 16f, HZ = 5.5f / 16f;

    public FluidTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            FluidTankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // ---- Fluid fill (translucent octagonal prism) ----
        if (be.isLocked()) {
            FluidStack fluid = be.getStoredFluid();

            float fillFrac = Math.max(0.01f, (float) be.getAmount() / be.getCapacity());
            float fillTop = FLUID_FLOOR + fillFrac * FLUID_H;
            float fillHeight = fillTop - FLUID_FLOOR;

            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite fluidSprite = sprite(ext.getStillTexture(fluid));

            int tint = ext.getTintColor(fluid);
            int fr = (tint >> 16) & 0xFF;
            int fg = (tint >> 8) & 0xFF;
            int fb = tint & 0xFF;
            int fa = (tint >> 24) & 0xFF;
            if (fa == 0) fa = 77; // ~30% opacity default

            int fluidLight = fluid.getFluidType().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

            VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

            // UV coords — use exact sprite atlas bounds to avoid sampling adjacent sprites.
            // V is lerped by fill fraction so the texture scales with the fluid level.
            float uLeft = fluidSprite.getU0();
            float uRight = fluidSprite.getU1();
            float vTop = fluidSprite.getV0();
            float vBottom = Mth.lerp(fillFrac, fluidSprite.getV0(), fluidSprite.getV1());

            // UV coords for top face: full sprite
            float uT0 = fluidSprite.getU0();
            float uT1 = fluidSprite.getU1();
            float vT0 = fluidSprite.getV0();
            float vT1 = fluidSprite.getV1();

            // 8 side faces — CCW winding when viewed from outside
            // Face A->B (north, normal 0,0,-1): emit B-bot, A-bot, A-top, B-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    BX,
                    FLUID_FLOOR,
                    BZ,
                    AX,
                    FLUID_FLOOR,
                    AZ,
                    AX,
                    fillTop,
                    AZ,
                    BX,
                    fillTop,
                    BZ,
                    0f,
                    0f,
                    -1f);

            // Face B->C (NE, normal 0.7071,0,-0.7071): emit C-bot, B-bot, B-top, C-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    CX,
                    FLUID_FLOOR,
                    CZ,
                    BX,
                    FLUID_FLOOR,
                    BZ,
                    BX,
                    fillTop,
                    BZ,
                    CX,
                    fillTop,
                    CZ,
                    0.7071f,
                    0f,
                    -0.7071f);

            // Face C->D (east, normal 1,0,0): emit D-bot, C-bot, C-top, D-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    DX,
                    FLUID_FLOOR,
                    DZ,
                    CX,
                    FLUID_FLOOR,
                    CZ,
                    CX,
                    fillTop,
                    CZ,
                    DX,
                    fillTop,
                    DZ,
                    1f,
                    0f,
                    0f);

            // Face D->E (SE, normal 0.7071,0,0.7071): emit E-bot, D-bot, D-top, E-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    EX,
                    FLUID_FLOOR,
                    EZ,
                    DX,
                    FLUID_FLOOR,
                    DZ,
                    DX,
                    fillTop,
                    DZ,
                    EX,
                    fillTop,
                    EZ,
                    0.7071f,
                    0f,
                    0.7071f);

            // Face E->F (south, normal 0,0,1): emit F-bot, E-bot, E-top, F-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    FX,
                    FLUID_FLOOR,
                    FZ,
                    EX,
                    FLUID_FLOOR,
                    EZ,
                    EX,
                    fillTop,
                    EZ,
                    FX,
                    fillTop,
                    FZ,
                    0f,
                    0f,
                    1f);

            // Face F->G (SW, normal -0.7071,0,0.7071): emit G-bot, F-bot, F-top, G-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    GX,
                    FLUID_FLOOR,
                    GZ,
                    FX,
                    FLUID_FLOOR,
                    FZ,
                    FX,
                    fillTop,
                    FZ,
                    GX,
                    fillTop,
                    GZ,
                    -0.7071f,
                    0f,
                    0.7071f);

            // Face G->H (west, normal -1,0,0): emit H-bot, G-bot, G-top, H-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    HX,
                    FLUID_FLOOR,
                    HZ,
                    GX,
                    FLUID_FLOOR,
                    GZ,
                    GX,
                    fillTop,
                    GZ,
                    HX,
                    fillTop,
                    HZ,
                    -1f,
                    0f,
                    0f);

            // Face H->A (NW, normal -0.7071,0,-0.7071): emit A-bot, H-bot, H-top, A-top
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uLeft,
                    vTop,
                    uRight,
                    vBottom,
                    AX,
                    FLUID_FLOOR,
                    AZ,
                    HX,
                    FLUID_FLOOR,
                    HZ,
                    HX,
                    fillTop,
                    HZ,
                    AX,
                    fillTop,
                    AZ,
                    -0.7071f,
                    0f,
                    -0.7071f);

            // Top face: octagonal prism cap, decomposed into 3 quads (all CCW from above).
            // UV maps [x=2/16..14/16, z=2/16..14/16] → full sprite.
            float topUa = Mth.lerp(3.5f / 12f, uT0, uT1); // x=5.5/16 (A,F)
            float topUb = Mth.lerp(8.5f / 12f, uT0, uT1); // x=10.5/16 (B,E)
            float topVh = Mth.lerp(3.5f / 12f, vT0, vT1); // z=5.5/16 (H,C)
            float topVg = Mth.lerp(8.5f / 12f, vT0, vT1); // z=10.5/16 (G,D)

            // North trapezoid: H(2,5.5)→C(14,5.5)→B(10.5,2)→A(5.5,2)
            quadFluidV(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    HX,
                    fillTop,
                    HZ,
                    uT0,
                    topVh,
                    CX,
                    fillTop,
                    CZ,
                    uT1,
                    topVh,
                    BX,
                    fillTop,
                    BZ,
                    topUb,
                    vT0,
                    AX,
                    fillTop,
                    AZ,
                    topUa,
                    vT0,
                    0f,
                    1f,
                    0f);

            // Middle rectangle: G(2,10.5)→D(14,10.5)→C(14,5.5)→H(2,5.5)
            quadFluid(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    uT0,
                    topVh,
                    uT1,
                    topVg,
                    GX,
                    fillTop,
                    GZ,
                    DX,
                    fillTop,
                    DZ,
                    CX,
                    fillTop,
                    CZ,
                    HX,
                    fillTop,
                    HZ,
                    0f,
                    1f,
                    0f);

            // South trapezoid: F(5.5,14)→E(10.5,14)→D(14,10.5)→G(2,10.5)
            quadFluidV(
                    translucent,
                    mat,
                    fr,
                    fg,
                    fb,
                    fa,
                    fluidLight,
                    packedOverlay,
                    FX,
                    fillTop,
                    FZ,
                    topUa,
                    vT1,
                    EX,
                    fillTop,
                    EZ,
                    topUb,
                    vT1,
                    DX,
                    fillTop,
                    DZ,
                    uT1,
                    topVg,
                    GX,
                    fillTop,
                    GZ,
                    uT0,
                    topVg,
                    0f,
                    1f,
                    0f);
        }

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
    // Geometry helpers
    // -------------------------------------------------------------------------

    private static TextureAtlasSprite sprite(ResourceLocation loc) {
        return Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(loc);
    }

    /**
     * Emits a single quad for the fluid prism using pre-computed atlas UV coordinates.
     * Vertex order: v0, v1, v2, v3 (CCW from outside).
     */
    @SuppressWarnings("java:S107")
    private static void quadFluid(
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

    /**
     * Emits a single quad with explicit per-vertex atlas UV coordinates.
     * Each vertex is described by (x,y,z, u,v) in the argument list.
     */
    @SuppressWarnings("java:S107")
    private static void quadFluidV(
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
        // -Y
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0,
                -1, 0);
        // +Y
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0,
                1, 0);
        // -Z (north)
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0,
                0, -1);
        // +Z (south)
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0,
                0, 1);
        // -X (west)
        quad(
                vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1,
                0, 0);
        // +X (east)
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
