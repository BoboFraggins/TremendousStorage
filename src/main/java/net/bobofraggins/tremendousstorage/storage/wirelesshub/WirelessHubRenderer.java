package net.bobofraggins.tremendousstorage.storage.wirelesshub;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders the Wireless Hub using baked models exported from the Blockbench source.
 *
 * <p>Two model parts:
 * <ul>
 *   <li>{@code wireless_hub_base} — the static body (base plate + tier highlight rings). Tier-tinted
 *       quads (tintindex=0) receive the {@link net.bobofraggins.tremendousstorage.shared.storage.StorageTier}
 *       color.
 *   <li>{@code wireless_hub_dish} — the dish/arm assembly. Rendered with an additional Y rotation
 *       that spins continuously (one full revolution every 40 ticks / 2 seconds) when the hub is
 *       attached to an active network.
 * </ul>
 *
 * <p>Both parts are oriented to the block's {@link WirelessHubBlock#FACING} direction before any
 * animation is applied.
 */
public class WirelessHubRenderer implements BlockEntityRenderer<WirelessHubBlockEntity> {

    private static final ModelResourceLocation BASE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/wireless_hub_base"));
    private static final ModelResourceLocation DISH_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/wireless_hub_dish"));

    /** Dish pivot is the block centre in all three axes (8/16 = 0.5). */
    private static final float PIVOT = 0.5f;

    public WirelessHubRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            WirelessHubBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BakedModel baseModel = mc.getModelManager().getModel(BASE_MODEL);
        BakedModel dishModel = mc.getModelManager().getModel(DISH_MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Level level = be.getLevel();
        if (level != null) {
            packedLight = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(WirelessHubBlock.FACING);
        float facingYRot =
                switch (facing) {
                    case WEST -> 90f;
                    case NORTH -> 180f;
                    case EAST -> 270f;
                    default -> 0f; // SOUTH
                };

        int color = be.getTier().getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RandomSource random = RandomSource.create();

        // ---- Static base ----
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        renderModel(
                consumer, poseStack.last(), baseModel, blockState, level, r, g, b, packedLight, packedOverlay, random);
        poseStack.popPose();

        // ---- Spinning dish ----
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        if (be.isConnected()) {
            float dishAngle = be.getDishAngle(partialTick);
            poseStack.translate(PIVOT, PIVOT, PIVOT);
            poseStack.mulPose(Axis.YP.rotationDegrees(dishAngle));
            poseStack.translate(-PIVOT, -PIVOT, -PIVOT);
        }
        renderModel(
                consumer, poseStack.last(), dishModel, blockState, level, r, g, b, packedLight, packedOverlay, random);
        poseStack.popPose();
    }

    private static void applyFacingRotation(PoseStack poseStack, float yDeg) {
        if (yDeg != 0f) {
            poseStack.translate(PIVOT, PIVOT, PIVOT);
            poseStack.mulPose(Axis.YP.rotationDegrees(yDeg));
            poseStack.translate(-PIVOT, -PIVOT, -PIVOT);
        }
    }

    private static void renderModel(
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
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.solid())) {
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
                qr = shade;
                qg = shade;
                qb = shade;
            }
            consumer.putBulkData(pose, quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
        }
    }
}
