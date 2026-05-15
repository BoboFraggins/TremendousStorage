package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Renders the Armory Cabinet with a two-phase door animation.
 *
 * <p>Phase 1 (open fractions 0→0.6): the locking wheel spins one full counterclockwise turn and
 * the two arm bars retract 3 px toward the center.
 *
 * <p>Phase 2 (open fractions 0.6→1.0): the door panel (and everything mounted to it) swings 90°
 * to the left.
 */
public class ArmoryCabinetRenderer
        implements BlockEntityRenderer<ArmoryCabinetBlockEntity>,
                IBlockEntityRendererExtension<ArmoryCabinetBlockEntity> {

    static final StandaloneModelKey<BlockStateModel> BODY = key("block/armory_cabinet_body");
    static final StandaloneModelKey<BlockStateModel> DOOR = key("block/armory_cabinet_door");
    static final StandaloneModelKey<BlockStateModel> ARM_TOP = key("block/armory_cabinet_arm_top");
    static final StandaloneModelKey<BlockStateModel> ARM_BOTTOM = key("block/armory_cabinet_arm_bottom");
    static final StandaloneModelKey<BlockStateModel> WHEEL = key("block/armory_cabinet_wheel");

    private static StandaloneModelKey<BlockStateModel> key(String path) {
        return new StandaloneModelKey<>(() -> "tremendousstorage:" + path);
    }

    private static final float PHASE1_END = 0.6f;
    private static final float PHASE2_START = 0.6f;
    private static final float ARM_RETRACT_PX = 3f / 16f;
    private static final float DOOR_SWING_DEG = 90f;
    private static final float WHEEL_SPIN_DEG = 360f;

    // Door hinge pivot — x=15, y=16, z=8 in Blockbench
    private static final float HINGE_X = 15f / 16f;
    private static final float HINGE_Z = 8f / 16f;

    // Wheel pivot — x=9, y=16, z=1 in Blockbench
    private static final float WHEEL_X = 9f / 16f;
    private static final float WHEEL_Y = 16f / 16f;
    private static final float WHEEL_Z = 1f / 16f;

    public ArmoryCabinetRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            ArmoryCabinetBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Vec3 cameraPos) {

        Minecraft mc = Minecraft.getInstance();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(ArmoryCabinetBlock.FACING);

        float facingYRot =
                switch (facing) {
                    case SOUTH -> 180f;
                    case EAST -> 270f;
                    case WEST -> 90f;
                    default -> 0f;
                };

        float open = Mth.lerp(partialTick, be.prevOpenFraction, be.openFraction);
        float p1 = Math.min(open / PHASE1_END, 1f);
        float p2 = Math.max((open - PHASE2_START) / (1f - PHASE2_START), 0f);

        RandomSource random = RandomSource.create();

        // Static body
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        renderModel(
                consumer,
                poseStack.last(),
                mc.getModelManager().getStandaloneModel(BODY),
                level,
                packedLight,
                packedOverlay,
                random);
        poseStack.popPose();

        // Door group (rotates in phase 2)
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        poseStack.translate(HINGE_X, 0, HINGE_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-p2 * DOOR_SWING_DEG));
        poseStack.translate(-HINGE_X, 0, -HINGE_Z);

        renderModel(
                consumer,
                poseStack.last(),
                mc.getModelManager().getStandaloneModel(DOOR),
                level,
                packedLight,
                packedOverlay,
                random);

        // Top arm — slides down 3 px in phase 1
        poseStack.pushPose();
        poseStack.translate(0, -ARM_RETRACT_PX * p1, 0);
        renderModel(
                consumer,
                poseStack.last(),
                mc.getModelManager().getStandaloneModel(ARM_TOP),
                level,
                packedLight,
                packedOverlay,
                random);
        poseStack.popPose();

        // Bottom arm — slides up 3 px in phase 1
        poseStack.pushPose();
        poseStack.translate(0, ARM_RETRACT_PX * p1, 0);
        renderModel(
                consumer,
                poseStack.last(),
                mc.getModelManager().getStandaloneModel(ARM_BOTTOM),
                level,
                packedLight,
                packedOverlay,
                random);
        poseStack.popPose();

        // Wheel — spins one full CCW turn in phase 1
        poseStack.pushPose();
        poseStack.translate(WHEEL_X, WHEEL_Y, WHEEL_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-WHEEL_SPIN_DEG * p1));
        poseStack.translate(-WHEEL_X, -WHEEL_Y, -WHEEL_Z);
        renderModel(
                consumer,
                poseStack.last(),
                mc.getModelManager().getStandaloneModel(WHEEL),
                level,
                packedLight,
                packedOverlay,
                random);
        poseStack.popPose();

        poseStack.popPose(); // end door group
    }

    private static void applyFacingRotation(PoseStack poseStack, float yDeg) {
        if (yDeg != 0f) {
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(yDeg));
            poseStack.translate(-0.5, -0.5, -0.5);
        }
    }

    private static void renderModel(
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
                    consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
                }
            }
            for (var quad : part.getQuads(null)) {
                Direction dir = quad.direction();
                float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
            }
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
