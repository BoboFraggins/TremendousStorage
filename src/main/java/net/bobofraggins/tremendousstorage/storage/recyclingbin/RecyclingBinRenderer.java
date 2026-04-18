package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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
 *   <li>Body — static, rendered with facing Y-rotation only.
 *   <li>Lid — animates open (rotates up by up to 90°) around the hinge at y=13/16, z=13/16.
 *   <li>Pedal — animates in sync (rotates down by up to 22.5°) around x=11/16, y=2/16, z=3/16.
 *   <li>Liquid — six cubes rendered procedurally using the Positive Vibes flowing texture,
 *       scaled in height to reflect current fill level.
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

    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "fluid/positive_vibes_flow");

    // Lid hinge pivot (y=13/16, z=13/16)
    private static final float LID_PIVOT_Y = 13f / 16f;
    private static final float LID_PIVOT_Z = 13f / 16f;

    // Pedal pivot (origin from bbmodel: x=11, y=2, z=3 in 0-16 space)
    private static final float PEDAL_PIVOT_X = 11f / 16f;
    private static final float PEDAL_PIVOT_Y = 2f / 16f;
    private static final float PEDAL_PIVOT_Z = 3f / 16f;

    // Liquid cube geometry — all 6 cubes share the same Y floor/ceiling in the model.
    // Each row: { x0, z0, x1, z1 }  (Y is handled separately)
    private static final float LIQUID_FLOOR = 1f / 16f;
    private static final float LIQUID_CEIL = 5f / 16f;
    private static final float[][] LIQUID_CUBES = {
        {2f / 16f, 6f / 16f, 5f / 16f, 10f / 16f},
        {1.5f / 16f, 7.5f / 16f, 2f / 16f, 8.5f / 16f},
        {4.5f / 16f, 4.5f / 16f, 5.5f / 16f, 5f / 16f},
        {4.5f / 16f, 11f / 16f, 5.5f / 16f, 11.5f / 16f},
        {3f / 16f, 5f / 16f, 6f / 16f, 6f / 16f},
        {3f / 16f, 10f / 16f, 6f / 16f, 11f / 16f},
    };

    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

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

        // Liquid fill
        float fillFraction = (float) be.getVibesAmount() / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
        if (fillFraction > 0f) {
            poseStack.pushPose();
            applyFacingRotation(poseStack, yRot);
            renderLiquid(poseStack.last().pose(), bufferSource, fillFraction, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    static void renderLiquid(
            Matrix4f mat, MultiBufferSource bufferSource, float fillFraction, int packedLight, int packedOverlay) {

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(FLOWING_TEXTURE);

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.POSITIVE_VIBES_TYPE.get());
        int tint = ext.getTintColor();
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        int a = (tint >> 24) & 0xFF;
        if (a == 0) a = 255;

        float topY = LIQUID_FLOOR + (LIQUID_CEIL - LIQUID_FLOOR) * fillFraction;

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        // Top faces use a culling render type — winding must be CCW from above (NW→SW→SE→NE)
        // so the geometric normal points up and the face is visible looking down into the bin.
        VertexConsumer vcCull = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        // Side faces use a non-culling render type to avoid z-fighting from shared interior
        // faces between adjacent cubes — each face is emitted once, visible from either side.
        VertexConsumer vcNoCull = bufferSource.getBuffer(RenderType.translucent());

        for (float[] c : LIQUID_CUBES) {
            float x0 = c[0], z0 = c[1], x1 = c[2], z1 = c[3];

            // Top face — CCW from above so normal faces +Y (visible looking down into bin).
            quad(
                    vcCull,
                    mat,
                    r,
                    g,
                    b,
                    a,
                    packedLight,
                    packedOverlay,
                    u0,
                    v0,
                    u1,
                    v1,
                    x0,
                    topY,
                    z0,
                    x0,
                    topY,
                    z1,
                    x1,
                    topY,
                    z1,
                    x1,
                    topY,
                    z0,
                    0,
                    1,
                    0);

            // Side faces — rendered without culling so interior shared faces don't z-fight.
            // North (-Z)
            quad(
                    vcNoCull,
                    mat,
                    r,
                    g,
                    b,
                    a,
                    packedLight,
                    packedOverlay,
                    u0,
                    v0,
                    u1,
                    v1,
                    x1,
                    topY,
                    z0,
                    x0,
                    topY,
                    z0,
                    x0,
                    LIQUID_FLOOR,
                    z0,
                    x1,
                    LIQUID_FLOOR,
                    z0,
                    0,
                    0,
                    -1);
            // South (+Z)
            quad(
                    vcNoCull,
                    mat,
                    r,
                    g,
                    b,
                    a,
                    packedLight,
                    packedOverlay,
                    u0,
                    v0,
                    u1,
                    v1,
                    x0,
                    topY,
                    z1,
                    x1,
                    topY,
                    z1,
                    x1,
                    LIQUID_FLOOR,
                    z1,
                    x0,
                    LIQUID_FLOOR,
                    z1,
                    0,
                    0,
                    1);
            // West (-X)
            quad(
                    vcNoCull,
                    mat,
                    r,
                    g,
                    b,
                    a,
                    packedLight,
                    packedOverlay,
                    u0,
                    v0,
                    u1,
                    v1,
                    x0,
                    topY,
                    z0,
                    x0,
                    topY,
                    z1,
                    x0,
                    LIQUID_FLOOR,
                    z1,
                    x0,
                    LIQUID_FLOOR,
                    z0,
                    -1,
                    0,
                    0);
            // East (+X)
            quad(
                    vcNoCull,
                    mat,
                    r,
                    g,
                    b,
                    a,
                    packedLight,
                    packedOverlay,
                    u0,
                    v0,
                    u1,
                    v1,
                    x1,
                    topY,
                    z1,
                    x1,
                    topY,
                    z0,
                    x1,
                    LIQUID_FLOOR,
                    z0,
                    x1,
                    LIQUID_FLOOR,
                    z1,
                    1,
                    0,
                    0);
        }
    }

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

    @Override
    public boolean shouldRenderOffScreen(RecyclingBinBlockEntity be) {
        return be.lidAngle > 0f || be.prevLidAngle > 0f;
    }

    @Override
    public AABB getRenderBoundingBox(RecyclingBinBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
    }
}
