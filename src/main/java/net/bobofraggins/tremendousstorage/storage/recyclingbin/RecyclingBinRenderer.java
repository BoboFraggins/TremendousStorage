package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

/**
 * Renders the Recycling Bin in four parts:
 * <ul>
 *   <li>Body — static baked model.
 *   <li>Lid — animates open (up to 90°) around the hinge at y=13/16, z=13/16.
 *   <li>Pedal — animates in sync (up to −22.5°) around x=11/16, y=2/16, z=3/16.
 *   <li>Fluid — single rectangular fill rendered like the Tank, using the Quantum Foam
 *       still texture. Always shown at a minimum of 1% to indicate the tank is present.
 * </ul>
 */
public class RecyclingBinRenderer
        implements BlockEntityRenderer<RecyclingBinBlockEntity>,
                IBlockEntityRendererExtension<RecyclingBinBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_body"));
    private static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_lid"));
    private static final ModelResourceLocation PEDAL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_pedal"));

    // Lid hinge pivot — back-top edge of the lid (y=13/16, z=13/16 in model space)
    private static final float LID_PIVOT_Y = 13f / 16f;
    private static final float LID_PIVOT_Z = 13f / 16f;

    // Pedal rotation pivot — from bbmodel origin x=11, y=2, z=3
    private static final float PEDAL_PIVOT_X = 11f / 16f;
    private static final float PEDAL_PIVOT_Y = 2f / 16f;
    private static final float PEDAL_PIVOT_Z = 3f / 16f;

    // Fluid interior bounds — derived from the glass wall elements in recycling_bin_body.json:
    //   front outer glass [8,1,4]→[14,13,5] : inner face at z=5, x spans 8–14
    //   back  outer glass [8,1,11]→[14,13,12]: inner face at z=11
    //   floor/ceiling from all glass panels  : y=1 to y=13
    private static final float FLUID_X0 = 8f / 16f;
    private static final float FLUID_X1 = 14f / 16f;
    private static final float FLUID_Z0 = 5f / 16f;
    private static final float FLUID_Z1 = 11f / 16f;
    private static final float FLUID_FLOOR = 1f / 16f;
    private static final float FLUID_CEIL = 13f / 16f;
    private static final float FLUID_H = FLUID_CEIL - FLUID_FLOOR;

    public RecyclingBinRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            RecyclingBinBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel lidModel = mc.getModelManager().getModel(LID_MODEL);
        BakedModel pedalModel = mc.getModelManager().getModel(PEDAL_MODEL);
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
        renderQuads(solidConsumer, poseStack.last(), bodyModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Lid
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(0.0, LID_PIVOT_Y, LID_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * 90f));
        poseStack.translate(0.0, -LID_PIVOT_Y, -LID_PIVOT_Z);
        renderQuads(solidConsumer, poseStack.last(), lidModel, blockState, level, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Pedal
        poseStack.pushPose();
        applyFacingRotation(poseStack, yRot);
        poseStack.translate(PEDAL_PIVOT_X, PEDAL_PIVOT_Y, PEDAL_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(openFraction * -22.5f));
        poseStack.translate(-PEDAL_PIVOT_X, -PEDAL_PIVOT_Y, -PEDAL_PIVOT_Z);
        renderQuads(solidConsumer, poseStack.last(), pedalModel, blockState, level, packedLight, packedOverlay, random);
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

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.QUANTUM_FOAM_TYPE.get());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(ext.getStillTexture());

        int tint = ext.getTintColor();
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 77;

        int fluidLight =
                Registration.QUANTUM_FOAM_TYPE.get().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

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
            BakedModel model,
            BlockState blockState,
            Level level,
            int packedLight,
            int packedOverlay,
            RandomSource random) {
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.cutout())) {
                float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.cutout())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            consumer.putBulkData(pose, quad, shade, shade, shade, 1f, packedLight, packedOverlay);
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
    public boolean shouldRenderOffScreen(RecyclingBinBlockEntity be) {
        return be.lidAngle > 0f || be.prevLidAngle > 0f;
    }

    @Override
    public AABB getRenderBoundingBox(RecyclingBinBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
