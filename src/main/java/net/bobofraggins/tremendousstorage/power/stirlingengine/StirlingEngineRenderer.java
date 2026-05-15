package net.bobofraggins.tremendousstorage.power.stirlingengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Renders the Stirling Engine body, animated flywheel, piston, and dynamic bridge connector. */
public class StirlingEngineRenderer
        implements BlockEntityRenderer<StirlingEngineBlockEntity>,
                IBlockEntityRendererExtension<StirlingEngineBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_body"), "standalone");
    private static final ModelResourceLocation FLYWHEEL_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_flywheel"),
            "standalone");
    private static final ModelResourceLocation PISTON_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_piston"),
            "standalone");
    private static final ModelResourceLocation BRIDGE_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_bridge"),
            "standalone");

    /** Flywheel rotation pivot — centre of the flywheel disc at X=1.5, Y=7.0, Z=8.0 px. */
    private static final float FW_X = 1.5f / 16f;

    private static final float FW_Y = 7.0f / 16f;
    private static final float FW_Z = 8.0f / 16f;

    /**
     * Max piston retraction in block space (0–1). Piston slides 4 px into the sheath at peak.
     */
    private static final float MAX_PISTON_RETRACT = 4.0f / 16f;

    /**
     * Flywheel anchor centre offset from the flywheel pivot in the YZ plane.
     * Anchor centre is at (Y=7.0, Z=11.0), pivot at (Y=7.0, Z=8.0) → dY=0, dZ=+3.
     */
    private static final float FA_REL_Y = 0.0f; // anchor at same Y as pivot

    private static final float FA_REL_Z = 3.0f / 16f; // anchor 3 px ahead of pivot in Z

    /** Arm (bridge) model geometry in model space. */
    private static final float ARM_CX = 4.5f / 16f; // arm centre X  (4.001+4.999)/2

    private static final float ARM_CY = 7.0f / 16f; // arm centre Y  (6.501+7.499)/2
    private static final float ARM_Z0 = 0.5f / 16f; // piston-end Z of arm (≈ 0.499/16)
    /** Distance in model space from ARM_Z0 to the flywheel-end attachment (Z=11.0 at rest):
     *  = faZ_rest − ARM_Z0_world = 11.0/16 − 0.5/16 = 10.5/16. Scale = 1 at rest. */
    private static final float ARM_REST_LEN = 10.5f / 16f;

    public StirlingEngineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            StirlingEngineBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel flywheelModel = mc.getModelManager().getModel(FLYWHEEL_MODEL);
        BakedModel pistonModel = mc.getModelManager().getModel(PISTON_MODEL);
        BakedModel bridgeModel = mc.getModelManager().getModel(BRIDGE_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        RandomSource random = RandomSource.create();

        // Tier color — applied as a tint to the flywheel disc elements.
        int tierColor = be.getTier().getColor();
        float tr = ((tierColor >> 16) & 0xFF) / 255f;
        float tg = ((tierColor >> 8) & 0xFF) / 255f;
        float tb = (tierColor & 0xFF) / 255f;

        // t in [0, 1) — fraction through the 40-tick cycle. Zero when not heated.
        float t = be.isHeated() ? (be.animationTicks + partialTick) / StirlingEngineBlockEntity.CYCLE_TICKS : 0f;
        float theta = Mth.TWO_PI * t;
        float cosT = Mth.cos(theta);
        float sinT = Mth.sin(theta);

        // Piston translation: sinusoidal retract toward the jacket once per cycle.
        // Starts retracted (pistonZ = MAX at t=0), extends to 0 at t=0.5, returns at t=1.
        float pistonZ = MAX_PISTON_RETRACT * (1f + cosT) / 2f;

        // Static body (base plate + cylinder + jacket trim)
        poseStack.pushPose();
        renderQuads(
                consumer,
                poseStack.last(),
                bodyModel,
                blockState,
                level,
                packedLight,
                packedOverlay,
                random,
                1f,
                1f,
                1f);
        poseStack.popPose();

        // Flywheel: one full revolution per cycle, rotating around the X axis through its centre.
        // Disc elements are tinted with the tier color.
        poseStack.pushPose();
        poseStack.translate(FW_X, FW_Y, FW_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(360f * t));
        poseStack.translate(-FW_X, -FW_Y, -FW_Z);
        renderQuads(
                consumer,
                poseStack.last(),
                flywheelModel,
                blockState,
                level,
                packedLight,
                packedOverlay,
                random,
                tr,
                tg,
                tb);
        poseStack.popPose();

        // Piston: sinusoidal retract-and-return once per cycle along +Z (toward the jacket).
        poseStack.pushPose();
        poseStack.translate(0f, 0f, pistonZ);
        renderQuads(
                consumer,
                poseStack.last(),
                pistonModel,
                blockState,
                level,
                packedLight,
                packedOverlay,
                random,
                1f,
                1f,
                1f);
        poseStack.popPose();

        // Arm connector: dynamically repositioned so its two ends stay in contact with the
        // flywheel anchor (which rotates) and the piston anchor (which translates).

        // Piston attachment: arm's piston-end (ARM_Z0) translated with the piston.
        float paX = ARM_CX;
        float paY = ARM_CY;
        float paZ = ARM_Z0 + pistonZ;

        // Flywheel attachment: anchor centre (dY=0, dZ=+3 from pivot), rotated with the flywheel.
        float faY = FW_Y + FA_REL_Y * cosT - FA_REL_Z * sinT;
        float faZ = FW_Z + FA_REL_Y * sinT + FA_REL_Z * cosT;

        float dy = faY - paY;
        float dz = faZ - paZ;

        // Flywheel rotates around the X axis, so both anchor X values are fixed; the arm only
        // moves in the YZ plane.
        float yzDist = Mth.sqrt(dy * dy + dz * dz);
        if (yzDist > 1e-4f) {
            poseStack.pushPose();
            // 1. Move local origin to piston attachment (arm's piston-end pivot).
            poseStack.translate(paX, paY, paZ);
            // 2. Rotate local Z axis in the YZ plane to point toward flywheel attachment.
            float pitch = (float) Math.atan2(-dy, dz);
            poseStack.mulPose(Axis.XP.rotation(pitch));
            // 3. Scale local Z so the arm spans the YZ distance between the two attachment points.
            poseStack.scale(1f, 1f, yzDist / ARM_REST_LEN);
            // 4. Shift arm model so its piston-end centre sits at local origin.
            poseStack.translate(-ARM_CX, -ARM_CY, -ARM_Z0);
            renderQuads(
                    consumer,
                    poseStack.last(),
                    bridgeModel,
                    blockState,
                    level,
                    packedLight,
                    packedOverlay,
                    random,
                    1f,
                    1f,
                    1f);
            poseStack.popPose();
        }
    }

    /**
     * Renders all quads of a baked model with directional shading. Quads that have a tint index ≥ 0
     * are multiplied by the supplied {@code r}/{@code g}/{@code b} tint (pass 1, 1, 1 for no tint).
     */
    private static void renderQuads(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedModel model,
            BlockState blockState,
            Level level,
            int packedLight,
            int packedOverlay,
            RandomSource random,
            float r,
            float g,
            float b) {
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.solid())) {
                float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
                float qr, qg, qb;
                if (quad.getTintIndex() >= 0) {
                    qr = r * shade;
                    qg = g * shade;
                    qb = b * shade;
                } else {
                    qr = qg = qb = shade;
                }
                consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.solid())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            float qr, qg, qb;
            if (quad.getTintIndex() >= 0) {
                qr = r * shade;
                qg = g * shade;
                qb = b * shade;
            } else {
                qr = qg = qb = shade;
            }
            consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
        }
    }
}
