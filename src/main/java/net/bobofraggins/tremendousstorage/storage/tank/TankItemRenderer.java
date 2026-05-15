package net.bobofraggins.tremendousstorage.storage.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class TankItemRenderer implements SpecialModelRenderer<TankItemRenderer.RenderData> {

    public record RenderData(StorageTier tier, @Nullable TankContents contents) {}

    @Override
    @Nullable
    public RenderData extractArgument(ItemStack stack) {
        StorageTier tier = StorageTier.WOOD;
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Tier")) {
                tier = StorageTier.fromId(tag.getString("Tier"));
            }
        }
        return new RenderData(tier, stack.get(Registration.TANK_CONTENTS.get()));
    }

    @Override
    public void render(
            @Nullable RenderData data,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            boolean hasFoil) {
        Minecraft mc = Minecraft.getInstance();

        StorageTier tier = data != null ? data.tier() : StorageTier.WOOD;
        BlockState renderState = Registration.TANK.get().defaultBlockState().setValue(TankBlock.TIER_PROP, tier);
        mc.getBlockRenderer()
                .renderSingleBlock(
                        renderState, poseStack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);

        TankContents contents = data != null ? data.contents() : null;
        if (contents == null || contents.isEmpty()) return;

        long capacity = tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
        float fillFrac = Math.max(0.01f, (float) contents.amount() / capacity);

        FluidStack fluid = contents.storedFluid();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());

        TextureAtlasSprite sprite = mc.getModelManager()
                .getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ext.getStillTexture(fluid));

        int tint = ext.getTintColor(fluid);
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 77;

        int fluidLight = fluid.getFluidType().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        float fillTop = TankRenderer.TANK_FLUID_FLOOR + fillFrac * TankRenderer.TANK_FLUID_H;
        float uL = sprite.getU0(), uR = sprite.getU1();
        float vT = sprite.getV0();
        float vB = Mth.lerp(fillFrac, sprite.getV0(), sprite.getV1());

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentItemSheet());
        TankRenderer.renderCubeFill(
                vc, poseStack.last().pose(), fr, fg, fb, fa, fluidLight, packedOverlay, uL, vT, uR, vB, fillTop);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new TankItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
