package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tank.TankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

public class NetworkInterfaceItemRenderer implements SpecialModelRenderer<StorageTier> {

    @Override
    public void getExtents(Consumer<Vector3fc> output) {}

    @Override
    @Nullable
    public StorageTier extractArgument(ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTagWithoutId();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getStringOr("Tier", ""));
            }
        }
        return StorageTier.WOOD;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void submit(
            @Nullable StorageTier tier,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int packedOverlay,
            boolean hasFoil,
            int tint) {
        if (tier == null) tier = StorageTier.WOOD;
        Minecraft mc = Minecraft.getInstance();

        BlockState renderState = Registration.NETWORK_INTERFACE
                .get()
                .defaultBlockState()
                .setValue(NetworkInterfaceBlock.TIER_PROP, tier);
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

        FluidStack vibes = new FluidStack(Registration.POSITIVE_VIBES_SOURCE.get(), 1000);
        net.minecraft.client.renderer.block.FluidModel fluidModel_ = net.minecraft.client.Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(net.bobofraggins.tremendousstorage.shared.register.Registration.POSITIVE_VIBES_SOURCE
                        .get()
                        .defaultFluidState());
        TextureAtlasSprite fluidSprite = fluidModel_.stillMaterial().sprite();

        int fluidTint = fluidModel_.fluidTintSource() != null
                ? fluidModel_
                        .fluidTintSource()
                        .colorAsStack(new net.neoforged.neoforge.fluids.FluidStack(
                                Registration.POSITIVE_VIBES_SOURCE.get(), 1))
                : 0xFFFFFFFF;
        int fr = (fluidTint >> 16) & 0xFF;
        int fg = (fluidTint >> 8) & 0xFF;
        int fb = fluidTint & 0xFF;
        int fa = (fluidTint >> 24) & 0xFF;
        if (fa == 0) fa = 160;
        fa /= 2;

        float fillFrac = 0.95f;
        float fillTop = TankRenderer.TANK_FLUID_FLOOR + fillFrac * TankRenderer.TANK_FLUID_H;
        float vB = Mth.lerp(fillFrac, fluidSprite.getV0(), fluidSprite.getV1());
        float uL = fluidSprite.getU0(), uR = fluidSprite.getU1(), vT = fluidSprite.getV0();

        int fluidLight = Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? 0xF000F0 : packedLight;
        final int ffrF = fr, ffgF = fg, ffbF = fb, ffaF = fa, flF = fluidLight, ovF = packedOverlay;
        collector.submitCustomGeometry(
                poseStack,
                Sheets.translucentItemSheet(),
                (pose, vc) -> TankRenderer.renderCubeFill(
                        vc, pose.pose(), ffrF, ffgF, ffbF, ffaF, flF, ovF, uL, vT, uR, vB, fillTop));

        double time = System.currentTimeMillis() / 1000.0;
        float bob = (float) Math.sin(time * Math.PI * 0.25) * 0.04f;
        float rotY = (float) ((time * 10.0) % 360.0);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.XP.rotationDegrees(3f));

        ItemStack brainStack = new ItemStack(Registration.BRAIN.get());
        ItemStackRenderState brainState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(brainState, brainStack, ItemDisplayContext.FIXED, null, null, 0);
        brainState.submit(poseStack, collector, packedLight, packedOverlay, 0);

        poseStack.popPose();
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<StorageTier> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<StorageTier> bake(SpecialModelRenderer.BakingContext context) {
            return new NetworkInterfaceItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<StorageTier>> type() {
            return MAP_CODEC;
        }
    }
}
