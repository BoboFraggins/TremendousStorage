package net.bobofraggins.intellistore.external.arsnouveau;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * Renders the Source Tank as a glass cylinder with a variable source fill level.
 *
 * <p>Source is rendered as a glowing teal/blue fill matching the Ars Nouveau aesthetic.
 * Geometry mirrors the Fluid Tank renderer exactly.
 */
public class SourceTankRenderer implements BlockEntityRenderer<SourceTankBlockEntity> {

    private static final ResourceLocation TANK_BODY =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/fluid_tank");
    private static final ResourceLocation JAR_GLASS =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/jar_glass");
    private static final ResourceLocation LAZURITE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/lazurite_block");

    // Geometry constants — identical to FluidTankRenderer
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

    private static final float STUB_D = 2f / 16f;
    private static final float STUB_MIN = 6f / 16f;
    private static final float STUB_MAX = 10f / 16f;

    // Ars Nouveau source colour: teal-blue glow
    private static final int SOURCE_R = 60;
    private static final int SOURCE_G = 180;
    private static final int SOURCE_B = 220;
    private static final int SOURCE_A = 210;

    public SourceTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            SourceTankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        TextureAtlasSprite bodySprite = sprite(TANK_BODY);
        TextureAtlasSprite glassSprite = sprite(JAR_GLASS);
        TextureAtlasSprite lazuriteSprite = sprite(LAZURITE_BLOCK);

        VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
        VertexConsumer translucent = bufferSource.getBuffer(RenderType.translucent());

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        // Lazurite rims
        drawCylinder(
                solid,
                mat,
                BODY_A0,
                0,
                BODY_A1,
                RIM_H,
                BODY_B0,
                BODY_B1,
                lazuriteSprite,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        drawCylinder(
                solid,
                mat,
                BODY_A0,
                1f - RIM_H,
                BODY_A1,
                1f,
                BODY_B0,
                BODY_B1,
                lazuriteSprite,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);

        // Source fill — always teal-blue; glow at full brightness
        if (be.getAmount() > 0) {
            float fillFrac = Math.max(MIN_FILL_FRAC, (float) be.getAmount() / SourceTankBlockEntity.CAPACITY);
            float fillTop = FLUID_FLOOR + fillFrac * FLUID_INTERIOR_H;
            // Source glows, so render at full brightness
            int FULL_BRIGHT = 0xF000F0;
            drawCylinder(
                    translucent,
                    mat,
                    FLUID_A0,
                    FLUID_FLOOR,
                    FLUID_A1,
                    fillTop,
                    FLUID_B0,
                    FLUID_B1,
                    bodySprite,
                    SOURCE_R,
                    SOURCE_G,
                    SOURCE_B,
                    SOURCE_A,
                    FULL_BRIGHT,
                    packedOverlay);
        }

        // Glass walls
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

        // Tube connector stubs
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

    private static final float SEAM_OFFSET = 0.001f;

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
        drawBox(vc, mat, zA0, y0 + SEAM_OFFSET, xA0, zA1, y1 - SEAM_OFFSET, xA1, sp, r, g, b, a, light, overlay);
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
