package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders the barrel body (baked model with proper AO + shading) plus the locked item and count
 * on its front face, matching the rendering approach used by the Filing Cabinet and Chest.
 */
public class BarrelRenderer
        implements BlockEntityRenderer<BarrelBlockEntity>, IBlockEntityRendererExtension<BarrelBlockEntity> {

    private static final ModelResourceLocation[] BODY_MODELS;
    private static final ModelResourceLocation[] ENDER_BODY_MODELS;

    static {
        StorageTier[] tiers = StorageTier.values();
        BODY_MODELS = new ModelResourceLocation[tiers.length];
        ENDER_BODY_MODELS = new ModelResourceLocation[tiers.length];
        for (StorageTier tier : tiers) {
            BODY_MODELS[tier.ordinal()] = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    "tremendousstorage", "block/barrels/barrel_body_" + tier.getId()));
            ENDER_BODY_MODELS[tier.ordinal()] = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    "tremendousstorage", "block/ender_barrels/ender_barrel_body_" + tier.getId()));
        }
    }

    public BarrelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            BarrelBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {

        Level level = be.getLevel();
        if (level != null) {
            light = LevelRenderer.getLightColor(level, be.getBlockPos());
        }

        Direction facing = be.getBlockState().getValue(BarrelBlock.FACING);
        float facingYRot =
                switch (facing) {
                    case SOUTH -> 180f;
                    case EAST -> 270f;
                    case WEST -> 90f;
                    default -> 0f;
                };

        // ── Barrel body ───────────────────────────────────────────────────────
        boolean isEnder = be instanceof EnderBarrelBlockEntity;
        ModelResourceLocation modelLoc = isEnder
                ? ENDER_BODY_MODELS[be.getTier().ordinal()]
                : BODY_MODELS[be.getTier().ordinal()];
        BakedModel bodyModel = Minecraft.getInstance().getModelManager().getModel(modelLoc);
        BlockState blockState = be.getBlockState();
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create();

        ps.pushPose();
        applyFacingRotation(ps, facingYRot);
        renderModel(consumer, ps.last(), bodyModel, blockState, level, light, overlay, random);
        ps.popPose();

        // ── Stored item + count ───────────────────────────────────────────────
        if (!be.isLocked()) return;
        ItemStack item = be.getStoredItem();
        if (item.isEmpty()) return;

        ps.pushPose();
        applyFacingRotation(ps, facingYRot);
        // Centre of the front (north) face, just outside the block surface
        ps.translate(0.5, 0.5, -0.002);

        ps.pushPose();
        ps.scale(0.75f, 0.75f, 0.001f);
        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(item, ItemDisplayContext.FIXED, light, overlay, ps, buffers, level, 0);
        ps.popPose();

        String label = CountFormat.format(be.getCount());
        Font font = Minecraft.getInstance().font;
        float textScale = 1f / 80f;
        ps.pushPose();
        ps.translate(0, -0.42, 0);
        ps.scale(textScale, -textScale, textScale);
        font.drawInBatch(
                label,
                -font.width(label) / 2f,
                0,
                -1,
                false,
                ps.last().pose(),
                buffers,
                Font.DisplayMode.NORMAL,
                0,
                light);
        ps.popPose();

        ps.popPose();
    }

    private static void applyFacingRotation(PoseStack ps, float yDeg) {
        if (yDeg != 0f) {
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yDeg));
            ps.translate(-0.5, -0.5, -0.5);
        }
    }

    private static void renderModel(
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
            for (var quad : model.getQuads(blockState, dir, random, ModelData.EMPTY, RenderType.solid())) {
                float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
                consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(blockState, null, random, ModelData.EMPTY, RenderType.solid())) {
            Direction dir = quad.getDirection();
            float shade = level != null ? level.getShade(dir, quad.isShade()) : 1f;
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }
}
