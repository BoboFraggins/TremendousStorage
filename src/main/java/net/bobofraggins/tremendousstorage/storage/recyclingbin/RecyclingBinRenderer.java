package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;

/**
 * Renders the Recycling Bin in four parts:
 * <ul>
 *   <li>Body — static model.
 *   <li>Lid — animates open (up to 90°) around the hinge at y=12/16, z=12/16.
 *   <li>Pedal — animates in sync (up to −22.5°) around x=12/16, y=1/16, z=4/16.
 *   <li>Fluid — Positive Vibes fill rendered via vertex consumer.
 * </ul>
 */
public class RecyclingBinRenderer
        implements BlockEntityRenderer<RecyclingBinBlockEntity>,
                IBlockEntityRendererExtension<RecyclingBinBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/recycling_bin_body");
    static final StandaloneModelKey<BlockStateModel> LID_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/recycling_bin_lid");
    static final StandaloneModelKey<BlockStateModel> PEDAL_MODEL_KEY =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/recycling_bin_pedal");

    // Lid hinge pivot — back-bottom edge of lid element [8,12,4]→[16,14,12]: y=12, z=12
    private static final float LID_PIVOT_Y = 12f / 16f;
    private static final float LID_PIVOT_Z = 12f / 16f;

    // Pedal rotation pivot — back-bottom edge of pedal element [11,1,1]→[13,2,4]: x=12, y=1, z=4
    private static final float PEDAL_PIVOT_X = 12f / 16f;
    private static final float PEDAL_PIVOT_Y = 1f / 16f;
    private static final float PEDAL_PIVOT_Z = 4f / 16f;

    // Fluid interior bounds — from the "fluid" guide element in the bbmodel: [1,1,5]→[7,9,11]
    private static final float FLUID_X0 = 1f / 16f;
    private static final float FLUID_X1 = 7f / 16f;
    private static final float FLUID_Z0 = 5f / 16f;
    private static final float FLUID_Z1 = 11f / 16f;
    private static final float FLUID_FLOOR = 1f / 16f;
    private static final float FLUID_CEIL = 9f / 16f;
    private static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    public RecyclingBinRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            RecyclingBinBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Vec3 cameraPos) {

        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL_KEY);
        BlockStateModel lidModel = mc.getModelManager().getStandaloneModel(LID_MODEL_KEY);
        BlockStateModel pedalModel = mc.getModelManager().getStandaloneModel(PEDAL_MODEL_KEY);
        VertexConsumer solidConsumer = bufferSource.getBuffer(RenderType.cutout());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(RecyclingBinBlock.FACING);
        float yRot = facingYRot(facing);

        float openFraction = Mth.lerp(partialTick, be.prevLidAngle, be.lidAngle);
        RandomSource random = RandomSource.create();

        // Body
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        renderQuads(solidConsumer, poseStack.last(), bodyModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Lid
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(0.0, LID_PIVOT_Y, LID_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -LID_PIVOT_Y, -LID_PIVOT_Z);
        renderQuads(solidConsumer, poseStack.last(), lidModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Pedal
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(PEDAL_PIVOT_X, PEDAL_PIVOT_Y, PEDAL_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * -22.5f));
        poseStack.translate(-PEDAL_PIVOT_X, -PEDAL_PIVOT_Y, -PEDAL_PIVOT_Z);
        renderQuads(solidConsumer, poseStack.last(), pedalModel, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Fluid fill — always rendered (minimum 1% so the tank area is always visible)
        float fillFraction = (float) be.getVibesAmount() / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        renderFluid(poseStack.last().pose(), bufferSource, fillFraction, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderFluid(
            Matrix4f mat, MultiBufferSource bufferSource, float fillFraction, int packedLight, int packedOverlay) {

        float fill = Math.max(0.01f, fillFraction);
        float fillTop = FLUID_FLOOR + fill * FLUID_H;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.POSITIVE_VIBES_TYPE.get());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ext.getStillTexture());

        int tint = ext.getTintColor();
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 77;

        int fluidLight =
                Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentItemSheet());

        float uL = sprite.getU0();
        float uR = sprite.getU1();
        float vT = sprite.getV0();
        float vB = Mth.lerp(fill, sprite.getV0(), sprite.getV1());

        // North (-Z)
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X0,
                fillTop,
                FLUID_Z0,
                FLUID_X1,
                fillTop,
                FLUID_Z0,
                0,
                0,
                -1);
        // South (+Z)
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X1,
                fillTop,
                FLUID_Z1,
                FLUID_X0,
                fillTop,
                FLUID_Z1,
                0,
                0,
                1);
        // West (-X)
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X0,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X0,
                fillTop,
                FLUID_Z1,
                FLUID_X0,
                fillTop,
                FLUID_Z0,
                -1,
                0,
                0);
        // East (+X)
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z1,
                FLUID_X1,
                FLUID_FLOOR,
                FLUID_Z0,
                FLUID_X1,
                fillTop,
                FLUID_Z0,
                FLUID_X1,
                fillTop,
                FLUID_Z1,
                1,
                0,
                0);
        // Top (+Y)
        quad(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                uL,
                vB,
                uR,
                vT,
                FLUID_X0,
                fillTop,
                FLUID_Z0,
                FLUID_X0,
                fillTop,
                FLUID_Z1,
                FLUID_X1,
                fillTop,
                FLUID_Z1,
                FLUID_X1,
                fillTop,
                FLUID_Z0,
                0,
                1,
                0);
    }

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    private static void applyFacingRotation(PoseStack poseStack, float yRot) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);
    }

    private static void renderQuads(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockStateModel model,
            Level level,
            int packedLight,
            int packedOverlay,
            RandomSource random) {
        List<BlockModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                    consumer.putBulkData(pose, quad, shade, shade, shade, 1f, packedLight, packedOverlay);
                }
            }
            for (var quad : part.getQuads(null)) {
                Direction dir = quad.direction();
                float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1f, packedLight, packedOverlay);
            }
        }
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

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(RecyclingBinBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
