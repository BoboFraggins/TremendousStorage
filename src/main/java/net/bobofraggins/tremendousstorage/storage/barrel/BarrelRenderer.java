package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Renders the barrel body (standalone model with proper AO + shading) plus the locked item and
 * count on its front face.
 */
public class BarrelRenderer
        implements BlockEntityRenderer<BarrelBlockEntity>, IBlockEntityRendererExtension<BarrelBlockEntity> {

    @SuppressWarnings("unchecked")
    static final StandaloneModelKey<BlockStateModel>[] BODY_MODELS =
            new StandaloneModelKey[StorageTier.values().length];

    @SuppressWarnings("unchecked")
    static final StandaloneModelKey<BlockStateModel>[] ENDER_BODY_MODELS =
            new StandaloneModelKey[StorageTier.values().length];

    @SuppressWarnings("unchecked")
    static final StandaloneModelKey<BlockStateModel>[] COMPACTING_BODY_MODELS =
            new StandaloneModelKey[StorageTier.values().length];

    static {
        for (StorageTier tier : StorageTier.values()) {
            final String id = tier.getId();
            BODY_MODELS[tier.ordinal()] =
                    new StandaloneModelKey<>(() -> "tremendousstorage:block/barrels/barrel_body_" + id);
            ENDER_BODY_MODELS[tier.ordinal()] =
                    new StandaloneModelKey<>(() -> "tremendousstorage:block/ender_barrels/ender_barrel_body_" + id);
            COMPACTING_BODY_MODELS[tier.ordinal()] =
                    new StandaloneModelKey<>(() -> "tremendousstorage:block/barrels_compacting/barrel_body_" + id);
        }
    }

    public BarrelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            BarrelBlockEntity be,
            float partialTick,
            PoseStack ps,
            MultiBufferSource buffers,
            int light,
            int overlay,
            Vec3 cameraPos) {

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

        // Barrel body
        boolean isEnder = be instanceof EnderBarrelBlockEntity;
        int tierIdx = be.getTier().ordinal();
        StandaloneModelKey<BlockStateModel> modelKey = be.hasCompactingUpgrade()
                ? COMPACTING_BODY_MODELS[tierIdx]
                : (isEnder ? ENDER_BODY_MODELS[tierIdx] : BODY_MODELS[tierIdx]);
        BlockStateModel bodyModel = Minecraft.getInstance().getModelManager().getStandaloneModel(modelKey);
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create();

        ps.pushPose();
        applyFacingRotation(ps, facingYRot);
        renderModel(consumer, ps.last(), bodyModel, level, light, overlay, random);
        ps.popPose();

        // Stored item + count
        if (!be.isLocked()) return;

        ps.pushPose();
        applyFacingRotation(ps, facingYRot);

        if (be.hasCompactingUpgrade()) {
            renderCompactingFace(be, ps, buffers, level, light, overlay);
        } else {
            ItemStack item = be.getStoredItem();
            ps.translate(0.5, 0.55, -0.01);
            ps.pushPose();

            ps.scale(0.5f, 0.5f, 0.001f);
            Minecraft.getInstance()
                    .getItemRenderer()
                    .renderStatic(
                            item, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, overlay, ps, buffers, level, 0);
            ps.popPose();

            String label = CountFormat.format(be.getCount());
            Font font = Minecraft.getInstance().font;
            float ts = 1f / 80f;
            ps.pushPose();
            ps.translate(0, -0.3, -0.0001);
            ps.scale(ts, -ts, ts);
            ps.mulPose(Axis.YP.rotationDegrees(180));
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
                    LightTexture.FULL_BRIGHT);
            if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
            ps.popPose();
        }

        ps.popPose();
    }

    private static void applyFacingRotation(PoseStack ps, float yDeg) {
        if (yDeg != 0f) {
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yDeg));
            ps.translate(-0.5, -0.5, -0.5);
        }
    }

    private static void renderCompactingFace(
            BarrelBlockEntity be, PoseStack ps, MultiBufferSource buffers, Level level, int light, int overlay) {
        int base = be.getBaseSlot();

        ItemStack s0 = ItemStack.EMPTY, s1 = ItemStack.EMPTY, s2 = ItemStack.EMPTY;
        long c0 = 0, c1 = 0, c2 = 0;

        if (base == 0) {
            s0 = be.getStoredItem();
            c0 = be.getCount();
            if (!be.getCompactTier1Item().isEmpty() && be.getCompactTier1Ratio() > 0) {
                s1 = be.getCompactTier1Item();
                c1 = be.getCount() / be.getCompactTier1Ratio();
            }
            if (!be.getCompactTier2Item().isEmpty() && be.getCompactTier1Ratio() > 0 && be.getCompactTier2Ratio() > 0) {
                s2 = be.getCompactTier2Item();
                c2 = be.getCount() / ((long) be.getCompactTier1Ratio() * be.getCompactTier2Ratio());
            }
        } else if (base == 1) {
            s1 = be.getStoredItem();
            c1 = be.getCount();
            if (!be.getCompactTier1Item().isEmpty() && be.getCompactTier1Ratio() > 0) {
                s2 = be.getCompactTier1Item();
                c2 = be.getCount() / be.getCompactTier1Ratio();
            }
        } else {
            s2 = be.getStoredItem();
            c2 = be.getCount();
        }

        if (!s2.isEmpty()) renderSmallItem(ps, buffers, s2, c2, 0.25f, 0.275f, level, light, overlay);
        if (!s1.isEmpty()) renderSmallItem(ps, buffers, s1, c1, 0.50f, 0.775f, level, light, overlay);
        if (!s0.isEmpty()) renderSmallItem(ps, buffers, s0, c0, 0.75f, 0.275f, level, light, overlay);
    }

    private static void renderSmallItem(
            PoseStack ps,
            MultiBufferSource buffers,
            ItemStack item,
            long count,
            float cx,
            float cy,
            Level level,
            int light,
            int overlay) {
        ps.pushPose();
        ps.translate(cx, cy, -0.01);

        ps.pushPose();
        ps.scale(0.25f, 0.25f, 0.001f);
        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(item, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, overlay, ps, buffers, level, 0);
        ps.popPose();

        String label = CountFormat.format(count);
        Font font = Minecraft.getInstance().font;
        float ts = 1f / 120f;
        ps.pushPose();
        ps.translate(0.0, -0.1f, -0.0001f);
        ps.scale(ts, -ts, ts);
        ps.mulPose(Axis.YP.rotationDegrees(180));
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
                LightTexture.FULL_BRIGHT);
        if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
        ps.popPose();

        ps.popPose();
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
}
