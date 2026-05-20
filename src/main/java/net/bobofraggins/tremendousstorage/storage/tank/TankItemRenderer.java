package net.bobofraggins.tremendousstorage.storage.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

public class TankItemRenderer implements SpecialModelRenderer<TankItemRenderer.RenderData> {

    public record RenderData(StorageTier tier, @Nullable TankContents contents) {}

    @Override
    public void getExtents(Consumer<Vector3fc> output) {}

    @Override
    @Nullable
    public RenderData extractArgument(ItemStack stack) {
        StorageTier tier = StorageTier.WOOD;
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTagWithoutId();
            if (tag.contains("Tier")) {
                tier = StorageTier.fromId(tag.getStringOr("Tier", ""));
            }
        }
        return new RenderData(tier, stack.get(Registration.TANK_CONTENTS.get()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void submit(
            @Nullable RenderData data,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int packedOverlay,
            boolean hasFoil,
            int tint) {
        Minecraft mc = Minecraft.getInstance();

        StorageTier tier = data != null ? data.tier() : StorageTier.WOOD;
        BlockState renderState = Registration.TANK.get().defaultBlockState().setValue(TankBlock.TIER_PROP, tier);
        {
            net.minecraft.client.renderer.block.dispatch.BlockStateModel blkModel =
                    mc.getModelManager().getBlockStateModelSet().get(renderState);
            java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> blkParts =
                    new java.util.ArrayList<>();
            blkModel.collectParts(net.minecraft.util.RandomSource.create(), blkParts);
            collector.submitCustomGeometry(
                    poseStack, net.minecraft.client.renderer.Sheets.cutoutBlockSheet(), (pose_, vc_) -> {
                        for (net.minecraft.client.renderer.block.dispatch.BlockStateModelPart part_ : blkParts) {
                            for (net.minecraft.core.Direction dir_ : net.minecraft.core.Direction.values()) {
                                for (var quad_ : part_.getQuads(dir_)) {
                                    com.mojang.blaze3d.vertex.QuadInstance qi_ =
                                            new com.mojang.blaze3d.vertex.QuadInstance();
                                    qi_.setColor(0xFFFFFFFF);
                                    qi_.setLightCoords(packedLight);
                                    qi_.setOverlayCoords(packedOverlay);
                                    vc_.putBakedQuad(pose_, quad_, qi_);
                                }
                            }
                            for (var quad_ : part_.getQuads(null)) {
                                com.mojang.blaze3d.vertex.QuadInstance qi_ =
                                        new com.mojang.blaze3d.vertex.QuadInstance();
                                qi_.setColor(0xFFFFFFFF);
                                qi_.setLightCoords(packedLight);
                                qi_.setOverlayCoords(packedOverlay);
                                vc_.putBakedQuad(pose_, quad_, qi_);
                            }
                        }
                    });
        }

        TankContents contents = data != null ? data.contents() : null;
        if (contents == null || contents.isEmpty()) return;

        long capacity = tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
        float fillFrac = Math.max(0.01f, (float) contents.amount() / capacity);

        FluidStack fluid = contents.storedFluid();
        net.minecraft.client.renderer.block.FluidModel fluidModel_ = net.minecraft.client.Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.getFluid().defaultFluidState());
        TextureAtlasSprite sprite = fluidModel_.stillMaterial().sprite();

        int fluidTint = fluidModel_.fluidTintSource() != null
                ? fluidModel_.fluidTintSource().colorAsStack(fluid)
                : 0xFFFFFFFF;
        int fr = (fluidTint >> 16) & 0xFF;
        int fg = (fluidTint >> 8) & 0xFF;
        int fb = fluidTint & 0xFF;
        int fa = (fluidTint >> 24) & 0xFF;
        if (fa == 0) fa = 77;

        int fluidLight = fluid.getFluidType().getLightLevel() > 0 ? 0xF000F0 : packedLight;

        float fillTop = TankRenderer.TANK_FLUID_FLOOR + fillFrac * TankRenderer.TANK_FLUID_H;
        float uL = sprite.getU0(), uR = sprite.getU1();
        float vT = sprite.getV0();
        float vB = Mth.lerp(fillFrac, sprite.getV0(), sprite.getV1());

        final int ffrF = fr, ffgF = fg, ffbF = fb, ffaF = fa, flF = fluidLight;
        final int overlayF = packedOverlay;
        collector.submitCustomGeometry(
                poseStack,
                Sheets.translucentBlockSheet(),
                (pose, vc) -> TankRenderer.renderCubeFill(
                        vc, pose.pose(), ffrF, ffgF, ffbF, ffaF, flF, overlayF, uL, vT, uR, vB, fillTop));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<RenderData> bake(SpecialModelRenderer.BakingContext context) {
            return new TankItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<RenderData>> type() {
            return MAP_CODEC;
        }
    }
}
