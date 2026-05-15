package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class RecyclingBinItemRenderer implements SpecialModelRenderer<Integer> {

    @SuppressWarnings("unchecked")
    private static final StandaloneModelKey<BlockStateModel>[] PART_MODELS = new StandaloneModelKey[] {
        RecyclingBinRenderer.BODY_MODEL_KEY, RecyclingBinRenderer.LID_MODEL_KEY, RecyclingBinRenderer.PEDAL_MODEL_KEY
    };

    @Override
    public void getExtents(Set<Vector3f> output) {}

    @Override
    @Nullable
    public Integer extractArgument(ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.copyTag();
        return tag.getInt("Vibes").orElse(0);
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
        VertexConsumer solidConsumer = bufferSource.getBuffer(RenderType.solid());
        var random = net.minecraft.util.RandomSource.create();

        for (StandaloneModelKey<BlockStateModel> key : PART_MODELS) {
            BlockStateModel model = mc.getModelManager().getStandaloneModel(key);
            List<BlockModelPart> parts = new ArrayList<>();
            random.setSeed(42L);
            model.collectParts(random, parts);
            for (BlockModelPart part : parts) {
                for (Direction dir : Direction.values()) {
                    for (var quad : part.getQuads(dir)) {
                        solidConsumer.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
                    }
                }
                for (var quad : part.getQuads(null)) {
                    solidConsumer.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
                }
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
