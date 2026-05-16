package net.bobofraggins.tremendousstorage.shared.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.joml.Matrix4f;

public abstract class AbstractTankRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T, AbstractTankRenderer.State>, IBlockEntityRendererExtension<T> {

    protected static final Identifier LAZURITE_BLOCK =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/lazurite_block");

    public static final float FLUID_FLOOR = 3f / 16f;
    public static final float FLUID_CEIL = 13f / 16f;
    public static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    private static final float STUB_D = 2f / 16f;
    private static final float STUB_MIN = 6f / 16f;
    private static final float STUB_MAX = 10f / 16f;

    protected static final float AX = 6f / 16f, AZ = 1f / 16f;
    protected static final float BX = 10f / 16f, BZ = 1f / 16f;
    protected static final float CX = 15f / 16f, CZ = 6f / 16f;
    protected static final float DX = 15f / 16f, DZ = 10f / 16f;
    protected static final float EX = 10f / 16f, EZ = 15f / 16f;
    protected static final float FX = 6f / 16f, FZ = 15f / 16f;
    protected static final float GX = 1f / 16f, GZ = 10f / 16f;
    protected static final float HX = 1f / 16f, HZ = 6f / 16f;

    public static class State extends BlockEntityRenderState {
        public boolean hasStub;
        public boolean[] stubDirs = new boolean[6];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            T be, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.hasStub = false;
        if (be.getLevel() != null) {
            BlockPos pos = be.getBlockPos();
            for (int i = 0; i < Direction.values().length; i++) {
                Direction dir = Direction.values()[i];
                state.stubDirs[i] =
                        be.getLevel().getBlockState(pos.relative(dir)).getBlock() instanceof TubeBlock;
                if (state.stubDirs[i]) state.hasStub = true;
            }
        }
    }

    protected abstract void renderFill(
            State state, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay);

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        int light = state.lightCoords;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        renderFill(state, poseStack, collector, light, overlay);
        poseStack.popPose();

        if (state.hasStub) {
            TextureAtlasSprite lazuriteSprite = sprite(LAZURITE_BLOCK);
            poseStack.pushPose();
            Matrix4f mat = poseStack.last().pose();
            for (int i = 0; i < Direction.values().length; i++) {
                if (state.stubDirs[i]) {
                    Direction dir = Direction.values()[i];
                    final Direction fDir = dir;
                    collector.submitCustomGeometry(
                            poseStack,
                            Sheets.cutoutBlockSheet(),
                            (pose, vc) -> drawStub(vc, pose.pose(), fDir, lazuriteSprite, light, overlay));
                }
            }
            poseStack.popPose();
        }
    }

    protected static TextureAtlasSprite sprite(Identifier loc) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)
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
        drawBox(vc, mat, x0, y0, z0, x1, y1, z1, sp, 255, 255, 255, 255, light, overlay, dir, dir.getOpposite());
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
        drawBox(vc, mat, x0, y0, z0, x1, y1, z1, sp, r, g, b, a, light, overlay, skipFace, null);
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
            Direction skipFace1,
            Direction skipFace2) {
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        if (skipFace1 != Direction.DOWN && skipFace2 != Direction.DOWN)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                    0, -1, 0);
        if (skipFace1 != Direction.UP && skipFace2 != Direction.UP)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                    0, 1, 0);
        if (skipFace1 != Direction.NORTH && skipFace2 != Direction.NORTH)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0,
                    0, 0, -1);
        if (skipFace1 != Direction.SOUTH && skipFace2 != Direction.SOUTH)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1,
                    0, 0, 1);
        if (skipFace1 != Direction.WEST && skipFace2 != Direction.WEST)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0,
                    -1, 0, 0);
        if (skipFace1 != Direction.EAST && skipFace2 != Direction.EAST)
            quad(
                    vc, mat, r, g, b, a, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1,
                    1, 0, 0);
    }

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
