package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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

/**
 * Renders the Armory Cabinet with a two-phase door animation.
 *
 * <p>Phase 1 (open fractions 0→0.6, 0.3 s at 0.1/tick): the locking wheel spins one full
 * counterclockwise turn and the two arm bars retract 3 px toward the center.
 *
 * <p>Phase 2 (open fractions 0.6→1.0, 0.2 s): the door panel (and everything mounted to it)
 * swings 90° to the left.
 *
 * <p>Model parts rendered:
 * <ul>
 *   <li>{@code armory_cabinet_body} — static back shell and guide rails in the frame.
 *   <li>{@code armory_cabinet_door} — door panel and door-mounted guide tabs.
 *   <li>{@code armory_cabinet_arm_top} — upper locking arm (translates down 3 px in phase 1).
 *   <li>{@code armory_cabinet_arm_bottom} — lower locking arm (translates up 3 px in phase 1).
 *   <li>{@code armory_cabinet_wheel} — combination lock wheel (rotates 360° CCW in phase 1).
 * </ul>
 */
public class ArmoryCabinetRenderer
        implements BlockEntityRenderer<ArmoryCabinetBlockEntity>,
                IBlockEntityRendererExtension<ArmoryCabinetBlockEntity> {

    private static final ModelResourceLocation BODY = standalone("block/armory_cabinet_body");
    private static final ModelResourceLocation DOOR = standalone("block/armory_cabinet_door");
    private static final ModelResourceLocation ARM_TOP = standalone("block/armory_cabinet_arm_top");
    private static final ModelResourceLocation ARM_BOTTOM = standalone("block/armory_cabinet_arm_bottom");
    private static final ModelResourceLocation WHEEL = standalone("block/armory_cabinet_wheel");

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("tremendousstorage", path));
    }

    // Animation constants
    private static final float PHASE1_END = 0.6f; // open fraction where phase 1 ends
    private static final float PHASE2_START = 0.6f;
    private static final float ARM_RETRACT_PX = 3f / 16f; // 3 pixels in block units
    private static final float DOOR_SWING_DEG = 90f;
    private static final float WHEEL_SPIN_DEG = 360f;

    // Door hinge — pivot set in Blockbench: x=15, y=16, z=8
    private static final float HINGE_X = 15f / 16f;
    private static final float HINGE_Z = 8f / 16f;

    // Wheel pivot — set in Blockbench: x=9, y=16, z=1
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
            int packedOverlay) {

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
                    default -> 0f; // NORTH
                };

        float open = Mth.lerp(partialTick, be.prevOpenFraction, be.openFraction);
        float p1 = Math.min(open / PHASE1_END, 1f); // 0→1 during phase 1
        float p2 = Math.max((open - PHASE2_START) / (1f - PHASE2_START), 0f); // 0→1 during phase 2

        RandomSource random = RandomSource.create();

        // ---- Static body ----
        BakedModel bodyModel = mc.getModelManager().getModel(BODY);
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        renderModel(consumer, poseStack.last(), bodyModel, state, level, be, packedLight, packedOverlay, random);
        poseStack.popPose();

        // ---- Door group (rotates in phase 2) ----
        // Apply door swing, then render all door-mounted parts inside it.
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);

        // Hinge is at the left edge of the door. "Swings to left" = the right edge swings
        // outward (toward viewer, then to the left). Implemented as positive Y rotation
        // around the hinge axis, which pulls the door panel counter-clockwise when viewed
        // from above.
        poseStack.translate(HINGE_X, 0, HINGE_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-p2 * DOOR_SWING_DEG));
        poseStack.translate(-HINGE_X, 0, -HINGE_Z);

        // Door panel + door guide tabs
        BakedModel doorModel = mc.getModelManager().getModel(DOOR);
        renderModel(consumer, poseStack.last(), doorModel, state, level, be, packedLight, packedOverlay, random);

        // Top arm — slides down 3 px in phase 1
        BakedModel armTopModel = mc.getModelManager().getModel(ARM_TOP);
        poseStack.pushPose();
        poseStack.translate(0, -ARM_RETRACT_PX * p1, 0);
        renderModel(consumer, poseStack.last(), armTopModel, state, level, be, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Bottom arm — slides up 3 px in phase 1
        BakedModel armBottomModel = mc.getModelManager().getModel(ARM_BOTTOM);
        poseStack.pushPose();
        poseStack.translate(0, ARM_RETRACT_PX * p1, 0);
        renderModel(consumer, poseStack.last(), armBottomModel, state, level, be, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Wheel — spins one full CCW turn in phase 1 (CCW from front = negative Z rotation)
        BakedModel wheelModel = mc.getModelManager().getModel(WHEEL);
        poseStack.pushPose();
        poseStack.translate(WHEEL_X, WHEEL_Y, WHEEL_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-WHEEL_SPIN_DEG * p1));
        poseStack.translate(-WHEEL_X, -WHEEL_Y, -WHEEL_Z);
        renderModel(consumer, poseStack.last(), wheelModel, state, level, be, packedLight, packedOverlay, random);
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
            BakedModel model,
            BlockState state,
            Level level,
            ArmoryCabinetBlockEntity be,
            int packedLight,
            int packedOverlay,
            RandomSource random) {
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(state, dir, random, ModelData.EMPTY, RenderType.solid())) {
                float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(state, null, random, ModelData.EMPTY, RenderType.solid())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
