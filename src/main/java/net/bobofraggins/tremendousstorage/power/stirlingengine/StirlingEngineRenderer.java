package net.bobofraggins.tremendousstorage.power.stirlingengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** Renders the Stirling Engine body, animated flywheel, piston, and dynamic bridge connector. */
public class StirlingEngineRenderer
        implements BlockEntityRenderer<StirlingEngineBlockEntity>,
                IBlockEntityRendererExtension<StirlingEngineBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_body");
    static final StandaloneModelKey<BlockStateModel> FLYWHEEL_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_flywheel");
    static final StandaloneModelKey<BlockStateModel> PISTON_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_piston");
    static final StandaloneModelKey<BlockStateModel> BRIDGE_MODEL =
            new StandaloneModelKey<>(() -> TremendousStorage.MODID + ":block/stirling_engine_bridge");

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
    private static final float FA_REL_Y = 0.0f;

    private static final float FA_REL_Z = 3.0f / 16f;

    /** Arm (bridge) model geometry in model space. */
    private static final float ARM_CX = 4.5f / 16f;

    private static final float ARM_CY = 7.0f / 16f;
    private static final float ARM_Z0 = 0.5f / 16f;
    /**
     * Distance in model space from ARM_Z0 to the flywheel-end attachment (Z=11.0 at rest):
     * = faZ_rest − ARM_Z0_world = 11.0/16 − 0.5/16 = 10.5/16. Scale = 1 at rest.
     */
    private static final float ARM_REST_LEN = 10.5f / 16f;

    public StirlingEngineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            StirlingEngineBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Vec3 cameraPos) {

        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(BODY_MODEL);
        BlockStateModel flywheelModel = mc.getModelManager().getStandaloneModel(FLYWHEEL_MODEL);
        BlockStateModel pistonModel = mc.getModelManager().getStandaloneModel(PISTON_MODEL);
        BlockStateModel bridgeModel = mc.getModelManager().getStandaloneModel(BRIDGE_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

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
        float pistonZ = MAX_PISTON_RETRACT * (1f + cosT) / 2f;

        // Static body
        poseStack.pushPose();
        renderQuads(consumer, poseStack.last(), bodyModel, level, packedLight, packedOverlay, random, 1f, 1f, 1f);
        poseStack.popPose();

        // Flywheel: one full revolution per cycle, rotating around the X axis through its centre.
        poseStack.pushPose();
        poseStack.translate(FW_X, FW_Y, FW_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(360f * t));
        poseStack.translate(-FW_X, -FW_Y, -FW_Z);
        renderQuads(consumer, poseStack.last(), flywheelModel, level, packedLight, packedOverlay, random, tr, tg, tb);
        poseStack.popPose();

        // Piston: sinusoidal retract-and-return once per cycle along +Z.
        poseStack.pushPose();
        poseStack.translate(0f, 0f, pistonZ);
        renderQuads(consumer, poseStack.last(), pistonModel, level, packedLight, packedOverlay, random, 1f, 1f, 1f);
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

        float yzDist = Mth.sqrt(dy * dy + dz * dz);
        if (yzDist > 1e-4f) {
            poseStack.pushPose();
            poseStack.translate(paX, paY, paZ);
            float pitch = (float) Math.atan2(-dy, dz);
            poseStack.mulPose(Axis.XP.rotation(pitch));
            poseStack.scale(1f, 1f, yzDist / ARM_REST_LEN);
            poseStack.translate(-ARM_CX, -ARM_CY, -ARM_Z0);
            renderQuads(consumer, poseStack.last(), bridgeModel, level, packedLight, packedOverlay, random, 1f, 1f, 1f);
            poseStack.popPose();
        }
    }

    /**
     * Renders all quads of a model with directional shading. Quads with tintIndex ≥ 0 are
     * multiplied by the supplied r/g/b tint (pass 1, 1, 1 for no tint).
     */
    private static void renderQuads(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockStateModel model,
            Level level,
            int packedLight,
            int packedOverlay,
            RandomSource random,
            float r,
            float g,
            float b) {
        List<BlockModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                    float qr, qg, qb;
                    if (quad.tintIndex() >= 0) {
                        qr = r * shade;
                        qg = g * shade;
                        qb = b * shade;
                    } else {
                        qr = qg = qb = shade;
                    }
                    consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
                }
            }
            for (var quad : part.getQuads(null)) {
                Direction dir = quad.direction();
                float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                float qr, qg, qb;
                if (quad.tintIndex() >= 0) {
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
}
