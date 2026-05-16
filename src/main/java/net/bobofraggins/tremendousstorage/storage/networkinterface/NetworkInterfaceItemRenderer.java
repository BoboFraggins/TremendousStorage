package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tank.TankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
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
            CompoundTag tag = customData.getUnsafe();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getStringOr("Tier", ""));
            }
        }
        return StorageTier.WOOD;
    }

    @Override
    public void submit(
            @Nullable StorageTier tier,
            ItemDisplayContext displayContext,
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
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        mc.getBlockRenderer().renderSingleBlock(renderState, poseStack, buffers, packedLight, packedOverlay);
        buffers.endBatch();

        FluidStack vibes = new FluidStack(Registration.POSITIVE_VIBES_SOURCE.get(), 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.POSITIVE_VIBES_TYPE.get());
        TextureAtlasSprite fluidSprite = mc.getAtlasManager()
                .getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ext.getStillTexture(vibes));

        int fluidTint = ext.getTintColor(vibes);
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

        int fluidLight =
                Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;
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

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new NetworkInterfaceItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
