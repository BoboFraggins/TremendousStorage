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
 *   <li>Fluid fill box (translucent, height proportional to fill fraction)
 *   <li>Tube-connector stubs on faces adjacent to a {@link TubeBlock} (solid lazurite)
 * </ul>
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {

    private static final ResourceLocation LAZURITE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/lazurite_block");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    // Fluid interior bounds (inside the glass jar)
    private static final float FLUID_MIN = 2f / 16f;
    private static final float FLUID_MAX = 14f / 16f;
    private static final float FLUID_FLOOR = 3f / 16f;
    private static final float FLUID_CEIL = 15f / 16f;
    private static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    private static final float MIN_FILL_FRAC = 0.05f;

    // Tube connector stub dimensions
    private static final float STUB_D = 2f / 16f;
    private static final float STUB_MIN = 6f / 16f;
    private static final float STUB_MAX = 10f / 16f;

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

        // ---- Fluid fill (translucent) ----
        if (be.isLocked()) {
            FluidStack fluid = be.getStoredFluid();

            float fillFrac = be.getAmount() > 0
                    ? Math.max(MIN_FILL_FRAC, (float) be.getAmount() / be.getCapacity())
                    : MIN_FILL_FRAC;
            float fillTop = FLUID_FLOOR + fillFrac * FLUID_H;

            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite fluidSprite = sprite(ext.getStillTexture(fluid));

            int tint = ext.getTintColor(fluid);
            int fr = (tint >> 16) & 0xFF;
            int fg = (tint >> 8) & 0xFF;
            int fb = tint & 0xFF;
            int fa = (tint >> 24) & 0xFF;
            if (fa == 0) fa = 255;

            int fluidLight = fluid.getFluidType().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

            VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
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
                    fluidLight,
                    packedOverlay);
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
