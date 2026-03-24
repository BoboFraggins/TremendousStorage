package net.bobofraggins.intellistore.storage.junkdrawer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders only the animated door of the Junk Drawer.
 *
 * <p>The static body is rendered by the chunk renderer via the blockstate model
 * ({@code junk_drawer_body}), which gives it proper AO and face culling.
 * This BESR handles only the door animation: pivot at the east edge of the door panel,
 * rotating around the Y axis (positive rotation opens the door outward toward the viewer).
 *
 * <p>The pivot in model space is at x={@value #HINGE_X_MODEL}, z={@value #HINGE_Z_MODEL},
 * derived from the Blockbench rotation origin shared by all door elements.
 */
public class JunkDrawerRenderer
        implements BlockEntityRenderer<JunkDrawerBlockEntity>, IBlockEntityRendererExtension<JunkDrawerBlockEntity> {

    static final ModelResourceLocation DOOR_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/junk_drawer_door"));

    /** Hinge x coordinate in model space (0–16), from Blockbench rotation origin. */
    private static final float HINGE_X_MODEL = 15f;
    /** Hinge z coordinate in model space (0–16), from Blockbench rotation origin. */
    private static final float HINGE_Z_MODEL = 4f;

    /** Y-rotation in degrees to apply for each facing direction (model default is NORTH). */
    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public JunkDrawerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            JunkDrawerBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel doorModel = mc.getModelManager().getModel(DOOR_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutoutMipped());

        // Sample light at the block's own position, same as the chunk renderer does for the body.
        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(JunkDrawerBlock.FACING);
        float yRot = facingYRot(facing);

        poseStack.pushPose();

        // Match the facing rotation applied to the body by the chunk renderer.
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Animate door: pivot at hinge in model space. The facing rotation above already
        // transforms the coordinate frame, so the pivot is always the model-space position.
        float openFraction = Mth.lerp(partialTick, be.prevDoorAngle, be.doorAngle);
        float hx = HINGE_X_MODEL / 16f;
        float hz = HINGE_Z_MODEL / 16f;
        poseStack.translate(hx, 0, hz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-openFraction * 90f));
        poseStack.translate(-hx, 0, -hz);

        int color = be.getTier().getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // Render each quad with per-face directional shading, matching what the chunk renderer
        // applies to the body. Tinted quads (tintindex >= 0) receive tier color × shade;
        // untinted quads receive shade only (white × shade).
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create();
        renderQuadsWithShading(
                consumer, pose, doorModel, blockState, level, r, g, b, packedLight, packedOverlay, random);

        poseStack.popPose();
    }

    /**
     * Renders all quads of the given model with per-face directional shading, matching the
     * chunk renderer's behavior. Tinted quads receive the tier color × shade; untinted quads
     * receive shade × white (1,1,1).
     */
    private static void renderQuadsWithShading(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedModel model,
            BlockState blockState,
            Level level,
            float r,
            float g,
            float b,
            int packedLight,
            int packedOverlay,
            RandomSource random) {
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.cutoutMipped())) {
                float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
                float qr, qg, qb;
                if (quad.getTintIndex() >= 0) {
                    qr = r * shade;
                    qg = g * shade;
                    qb = b * shade;
                } else {
                    qr = shade;
                    qg = shade;
                    qb = shade;
                }
                consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
            }
        }
        // Unculled quads
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.cutoutMipped())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            float qr, qg, qb;
            if (quad.getTintIndex() >= 0) {
                qr = r * shade;
                qg = g * shade;
                qb = b * shade;
            } else {
                qr = shade;
                qg = shade;
                qb = shade;
            }
            consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(JunkDrawerBlockEntity be) {
        return be.doorAngle > 0f || be.prevDoorAngle > 0f;
    }

    @Override
    public AABB getRenderBoundingBox(JunkDrawerBlockEntity be) {
        Direction facing = be.getBlockState().getValue(JunkDrawerBlock.FACING);
        AABB base = new AABB(be.getBlockPos());
        return switch (facing) {
            case SOUTH -> base.expandTowards(0, 0, 1).expandTowards(-1, 0, 0);
            case EAST -> base.expandTowards(1, 0, 0).expandTowards(0, 0, -1);
            case WEST -> base.expandTowards(-1, 0, 0).expandTowards(0, 0, 1);
            default -> base.expandTowards(0, 0, -1).expandTowards(1, 0, 0); // NORTH
        };
    }
}
