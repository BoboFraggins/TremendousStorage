package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Renders the Wireless Hub using two model parts:
 * <ul>
 *   <li>{@code wireless_hub_base} — static body; tier-tinted quads (tintindex=0) receive the tier color.
 *   <li>{@code wireless_hub_dish} — dish/arm assembly, spinning continuously when connected.
 * </ul>
 */
public class WirelessHubRenderer
        implements BlockEntityRenderer<WirelessHubBlockEntity>, IBlockEntityRendererExtension<WirelessHubBlockEntity> {

    public static final StandaloneModelKey<BlockStateModel> BASE_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/wireless_hub_base");
    public static final StandaloneModelKey<BlockStateModel> DISH_MODEL =
            new StandaloneModelKey<>(() -> "tremendousstorage:block/wireless_hub_dish");

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
            int packedOverlay,
            Vec3 cameraPos) {

        Minecraft mc = Minecraft.getInstance();
        BlockStateModel baseModel = mc.getModelManager().getStandaloneModel(BASE_MODEL);
        BlockStateModel dishModel = mc.getModelManager().getStandaloneModel(DISH_MODEL);
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
                    default -> 0f;
                };

        int color = be.getTier().getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RandomSource random = RandomSource.create();

        // Static base
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        renderModel(consumer, poseStack.last(), baseModel, level, r, g, b, packedLight, packedOverlay, random);
        poseStack.popPose();

        // Spinning dish
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingYRot);
        if (be.isConnected()) {
            float dishAngle = be.getDishAngle(partialTick);
            poseStack.translate(PIVOT, PIVOT, PIVOT);
            poseStack.mulPose(Axis.YP.rotationDegrees(dishAngle));
            poseStack.translate(-PIVOT, -PIVOT, -PIVOT);
        }
        renderModel(consumer, poseStack.last(), dishModel, level, r, g, b, packedLight, packedOverlay, random);
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
            BlockStateModel model,
            Level level,
            float r,
            float g,
            float b,
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
                    float qr, qg, qb;
                    if (quad.tintIndex() >= 0) {
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
            for (var quad : part.getQuads(null)) {
                Direction dir = quad.direction();
                float shade = level != null ? level.getShade(dir, quad.shade()) : 1f;
                float qr, qg, qb;
                if (quad.tintIndex() >= 0) {
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
}
