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
import net.neoforged.neoforge.client.model.data.ModelData;

/** Renders the Stirling Engine body, animated flywheel, piston, and dynamic bridge connector. */
public class StirlingEngineRenderer implements BlockEntityRenderer<StirlingEngineBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_body"));
    private static final ModelResourceLocation FLYWHEEL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_flywheel"));
    private static final ModelResourceLocation PISTON_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_piston"));
    private static final ModelResourceLocation BRIDGE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_bridge"));

    /** Flywheel rotation pivot in block space (0–1). Centre of flywheel disc at X=3.225, Y=7.6, Z=4.6. */
    private static final float FW_X = 3.225f / 16f;

    private static final float FW_Y = 7.6f / 16f;
    private static final float FW_Z = 4.6f / 16f;

    /**
     * Max piston retraction in block space (0–1). Piston connector near-face at Z=3.4/16; jacket
     * inner opening at Z≈7.76/16. Retract up to 4.0/16 leaving small clearance.
     */
    private static final float MAX_PISTON_RETRACT = 4.0f / 16f;

    /**
     * Flywheel attachment point (centre of the flywheel-side connector's Z=7.55 face) expressed as
     * an offset relative to the flywheel pivot, in YZ-plane block units (/16).
     */
    private static final float FA_REL_X = (4.8f - 3.225f) / 16f; // 1.575/16

    private static final float FA_REL_Y = (5.525f - 5.6f) / 16f; // -0.075/16
    private static final float FA_REL_Z = (7.55f - 4.6f) / 16f; // 2.95/16

    /** Bridge model — piston-end face centre in model space. */
    private static final float BRIDGE_CX = (5.55f + 6.0f) / 2f / 16f; // 5.775/16

    private static final float BRIDGE_CY = (7.3f + 7.75f) / 2f / 16f; // 7.525/16
    private static final float BRIDGE_Z0 = 3.15f / 16f; // piston-end Z
    private static final float BRIDGE_Z1 = 7.65f / 16f; // flywheel-end Z
    private static final float BRIDGE_REST_LEN = BRIDGE_Z1 - BRIDGE_Z0; // 4.2/16

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
        float pistonZ = MAX_PISTON_RETRACT * (1f - Mth.cos(Mth.TWO_PI * t)) / 2f;

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

        // Bridge connector: dynamically repositioned so its two ends stay in contact with the
        // flywheel-side connector (which rotates) and the piston-side connector (which translates).

        // Piston attachment: centre of bridge at its piston-end face, shifted by piston translation.
        float paX = BRIDGE_CX;
        float paY = BRIDGE_CY;
        float paZ = BRIDGE_Z0 + pistonZ;

        // Flywheel attachment: centre of the flywheel-side connector's Z=7.55 face, rotated with the
        // flywheel around the flywheel pivot.
        float faY = FW_Y + FA_REL_Y * cosT - FA_REL_Z * sinT;
        float faZ = FW_Z + FA_REL_Y * sinT + FA_REL_Z * cosT;

        float dy = faY - paY;
        float dz = faZ - paZ;

        // Both attachment X values are fixed (flywheel rotates around X axis), so the bridge only
        // moves in the YZ plane. Ignore dx and rotate purely around the X axis.
        float yzDist = Mth.sqrt(dy * dy + dz * dz);
        if (yzDist > 1e-4f) {
            poseStack.pushPose();
            // 1. Move local origin to piston attachment (will be the bridge's piston-end pivot).
            poseStack.translate(paX, paY, paZ);
            // 2. Rotate local Z axis in the YZ plane to point toward flywheel attachment.
            float pitch = (float) Math.atan2(-dy, dz);
            poseStack.mulPose(Axis.XP.rotation(pitch));
            // 3. Scale local Z so bridge spans the YZ distance between the two attachment points.
            poseStack.scale(1f, 1f, yzDist / BRIDGE_REST_LEN);
            // 4. Shift bridge model so its piston-end face centre sits at local origin.
            poseStack.translate(-BRIDGE_CX, -BRIDGE_CY, -BRIDGE_Z0);
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
