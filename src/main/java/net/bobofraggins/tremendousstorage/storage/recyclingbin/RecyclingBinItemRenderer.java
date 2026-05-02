package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
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

/**
 * Custom item renderer for the Recycling Bin.
 *
 * <p>Renders the three baked component models (body, lid, pedal) at rest position
 * and, if the item's saved data contains Positive Vibes, overlays the fluid cubes
 * using the actual Positive Vibes flowing texture.
 */
public class RecyclingBinItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_body"));
    private static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_lid"));
    private static final ModelResourceLocation PEDAL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_pedal"));

    private static final ModelResourceLocation[] PART_MODELS = {BODY_MODEL, LID_MODEL, PEDAL_MODEL};

    private static RecyclingBinItemRenderer instance;

    public RecyclingBinItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    /** Lazily constructed singleton — must be called from the render thread. */
    public static RecyclingBinItemRenderer getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new RecyclingBinItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        var solidConsumer = bufferSource.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create();

        // Render body, lid, and pedal at rest (closed) position.
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

        // Render fluid if the item has stored Positive Vibes.
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.getUnsafe();
        int vibes = tag.getInt("Vibes");
        if (vibes <= 0) return;

        float fillFraction = (float) vibes / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
        RecyclingBinRenderer.renderFluid(
                poseStack.last().pose(), bufferSource, fillFraction, packedLight, packedOverlay);
    }
}
