package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class RecyclingBinItemRenderer implements SpecialModelRenderer<Integer> {

    private static final ModelResourceLocation BODY_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_body"), "standalone");
    private static final ModelResourceLocation LID_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_lid"), "standalone");
    private static final ModelResourceLocation PEDAL_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_pedal"), "standalone");

    private static final ModelResourceLocation[] PART_MODELS = {BODY_MODEL, LID_MODEL, PEDAL_MODEL};

    @Override
    @Nullable
    public Integer extractArgument(ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.copyTag();
        return tag.getInt("Vibes");
    }

    @Override
    public void render(
            @Nullable Integer vibes,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            boolean hasFoil) {
        Minecraft mc = Minecraft.getInstance();
        var solidConsumer = bufferSource.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create();

        for (ModelResourceLocation loc : PART_MODELS) {
            BakedModel model = mc.getModelManager().getModel(loc);
            for (Direction dir : Direction.values()) {
                random.setSeed(42L);
                for (var quad : model.getQuads(null, dir, random, ModelData.EMPTY, RenderType.solid())) {
                    solidConsumer.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
                }
            }
            random.setSeed(42L);
            for (var quad : model.getQuads(null, null, random, ModelData.EMPTY, RenderType.solid())) {
                solidConsumer.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
            }
        }

        if (vibes == null || vibes <= 0) return;

        float fillFraction = (float) vibes / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
        RecyclingBinRenderer.renderFluid(
                poseStack.last().pose(), bufferSource, fillFraction, packedLight, packedOverlay);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new RecyclingBinItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
