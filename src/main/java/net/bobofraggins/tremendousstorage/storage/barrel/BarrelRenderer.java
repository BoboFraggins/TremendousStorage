package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.storage.enderbarrel.EnderBarrelBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BarrelRenderer
        implements BlockEntityRenderer<BarrelBlockEntity, BarrelRenderer.State>,
                IBlockEntityRendererExtension<BarrelBlockEntity> {

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

    public static class State extends BlockEntityRenderState {
        public float facingYRot;
        public StandaloneModelKey<BlockStateModel> modelKey;
        public boolean isLocked;
        public boolean hasCompactingUpgrade;
        public ItemStack storedItem = ItemStack.EMPTY;
        public long count;
        public int baseSlot;
        public ItemStack s0 = ItemStack.EMPTY, s1 = ItemStack.EMPTY, s2 = ItemStack.EMPTY;
        public long c0, c1, c2;
    }

    public BarrelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BarrelBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        Direction facing = be.getBlockState().getValue(BarrelBlock.FACING);
        state.facingYRot = switch (facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
        boolean isEnder = be instanceof EnderBarrelBlockEntity;
        int tierIdx = be.getTier().ordinal();
        state.hasCompactingUpgrade = be.hasCompactingUpgrade();
        state.modelKey = state.hasCompactingUpgrade
                ? COMPACTING_BODY_MODELS[tierIdx]
                : (isEnder ? ENDER_BODY_MODELS[tierIdx] : BODY_MODELS[tierIdx]);
        state.isLocked = be.isLocked();
        if (state.isLocked) {
            state.storedItem = be.getStoredItem();
            state.count = be.getCount();
            if (state.hasCompactingUpgrade) {
                state.baseSlot = be.getBaseSlot();
                state.s0 = state.s1 = state.s2 = ItemStack.EMPTY;
                state.c0 = state.c1 = state.c2 = 0;
                if (state.baseSlot == 0) {
                    state.s0 = be.getStoredItem();
                    state.c0 = be.getCount();
                    if (!be.getCompactTier1Item().isEmpty() && be.getCompactTier1Ratio() > 0) {
                        state.s1 = be.getCompactTier1Item();
                        state.c1 = be.getCount() / be.getCompactTier1Ratio();
                    }
                    if (!be.getCompactTier2Item().isEmpty()
                            && be.getCompactTier1Ratio() > 0
                            && be.getCompactTier2Ratio() > 0) {
                        state.s2 = be.getCompactTier2Item();
                        state.c2 = be.getCount() / ((long) be.getCompactTier1Ratio() * be.getCompactTier2Ratio());
                    }
                } else if (state.baseSlot == 1) {
                    state.s1 = be.getStoredItem();
                    state.c1 = be.getCount();
                    if (!be.getCompactTier1Item().isEmpty() && be.getCompactTier1Ratio() > 0) {
                        state.s2 = be.getCompactTier1Item();
                        state.c2 = be.getCount() / be.getCompactTier1Ratio();
                    }
                } else {
                    state.s2 = be.getStoredItem();
                    state.c2 = be.getCount();
                }
            }
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel bodyModel = mc.getModelManager().getStandaloneModel(state.modelKey);
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;
        RandomSource random = RandomSource.create();

        ps.pushPose();
        applyFacingRotation(ps, state.facingYRot);
        List<BlockStateModelPart> bodyParts = collectParts(bodyModel, random);
        collector.submitCustomGeometry(
                ps,
                Sheets.cutoutBlockSheet(),
                (pose, consumer) -> renderModel(consumer, pose, bodyParts, light, overlay));
        ps.popPose();

        if (!state.isLocked) return;

        // Use main buffer source for item and text rendering
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        ps.pushPose();
        applyFacingRotation(ps, state.facingYRot);

        if (state.hasCompactingUpgrade) {
            if (!state.s2.isEmpty())
                renderSmallItem(ps, collector, buffers, state.s2, state.c2, 0.25f, 0.275f, overlay);
            if (!state.s1.isEmpty())
                renderSmallItem(ps, collector, buffers, state.s1, state.c1, 0.50f, 0.775f, overlay);
            if (!state.s0.isEmpty())
                renderSmallItem(ps, collector, buffers, state.s0, state.c0, 0.75f, 0.275f, overlay);
        } else {
            ps.translate(0.5, 0.55, -0.01);
            ps.pushPose();
            ps.scale(0.5f, 0.5f, 0.001f);
            ItemStackRenderState irs = new ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(irs, state.storedItem, ItemDisplayContext.FIXED, null, null, 0);
            irs.submit(ps, collector, 0xF000F0, overlay, 0);
            ps.popPose();
            String label = CountFormat.format(state.count);
            Font font = mc.font;
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
                    0xF000F0);
            buffers.endBatch();
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

    private static void renderSmallItem(
            PoseStack ps,
            SubmitNodeCollector collector,
            MultiBufferSource.BufferSource buffers,
            ItemStack item,
            long count,
            float cx,
            float cy,
            int overlay) {
        ps.pushPose();
        ps.translate(cx, cy, -0.01);
        ps.pushPose();
        ps.scale(0.25f, 0.25f, 0.001f);
        ItemStackRenderState irs = new ItemStackRenderState();
        Minecraft.getInstance()
                .getItemModelResolver()
                .updateForTopItem(irs, item, ItemDisplayContext.FIXED, null, null, 0);
        irs.submit(ps, collector, 0xF000F0, overlay, 0);
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
                0xF000F0);
        buffers.endBatch();
        ps.popPose();
        ps.popPose();
    }

    private static List<BlockStateModelPart> collectParts(BlockStateModel model, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        random.setSeed(42L);
        model.collectParts(random, parts);
        return parts;
    }

    private static void renderModel(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BlockStateModelPart> parts,
            int packedLight,
            int overlay) {
        for (BlockStateModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                for (var quad : part.getQuads(dir)) {
                    QuadInstance qi = new QuadInstance();
                    qi.setColor(0xFFFFFFFF);
                    qi.setLightCoords(packedLight);
                    qi.setOverlayCoords(overlay);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
            for (var quad : part.getQuads(null)) {
                QuadInstance qi = new QuadInstance();
                qi.setColor(0xFFFFFFFF);
                qi.setLightCoords(packedLight);
                qi.setOverlayCoords(overlay);
                consumer.putBakedQuad(pose, quad, qi);
            }
        }
    }
}
