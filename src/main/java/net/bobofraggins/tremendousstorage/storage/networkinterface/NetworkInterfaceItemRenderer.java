package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tank.TankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class NetworkInterfaceItemRenderer implements SpecialModelRenderer<StorageTier> {

    private final ItemStackRenderState brainRenderState = new ItemStackRenderState();

    @Override
    @Nullable
    public StorageTier extractArgument(ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getString("Tier"));
            }
        }
        return StorageTier.WOOD;
    }

    @Override
    public void render(
            @Nullable StorageTier tier,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            boolean hasFoil) {
        if (tier == null) tier = StorageTier.WOOD;
        Minecraft mc = Minecraft.getInstance();

        BlockState renderState = Registration.NETWORK_INTERFACE
                .get()
                .defaultBlockState()
                .setValue(NetworkInterfaceBlock.TIER_PROP, tier);
        mc.getBlockRenderer()
                .renderSingleBlock(
                        renderState, poseStack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);

        // Positive Vibes fill at 95%
        FluidStack vibes = new FluidStack(Registration.POSITIVE_VIBES_SOURCE.get(), 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.POSITIVE_VIBES_TYPE.get());
        ResourceLocation stillTex = ext.getStillTexture(vibes);
        TextureAtlasSprite fluidSprite = mc.getModelManager()
                .getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .getSprite(stillTex);

        int tint = ext.getTintColor(vibes);
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 160;
        fa /= 2;

        float fillFrac = 0.95f;
        float fillTop = TankRenderer.TANK_FLUID_FLOOR + fillFrac * TankRenderer.TANK_FLUID_H;
        float vB = Mth.lerp(fillFrac, fluidSprite.getV0(), fluidSprite.getV1());

        int fluidLight =
                Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentItemSheet());
        TankRenderer.renderCubeFill(
                vc,
                poseStack.last().pose(),
                fr,
                fg,
                fb,
                fa,
                fluidLight,
                packedOverlay,
                fluidSprite.getU0(),
                fluidSprite.getV0(),
                fluidSprite.getU1(),
                vB,
                fillTop);

        // Animated floating brain
        double time = System.currentTimeMillis() / 1000.0;
        float bob = (float) Math.sin(time * Math.PI * 0.25) * 0.04f;
        float rotY = (float) ((time * 10.0) % 360.0);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.XP.rotationDegrees(3f));

        ItemStack brainStack = new ItemStack(Registration.BRAIN.get());
        mc.getItemModelResolver()
                .updateForTopItem(brainRenderState, brainStack, ItemDisplayContext.FIXED, false, null, null, 0);
        brainRenderState.render(poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new NetworkInterfaceItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
