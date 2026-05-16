package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

public class RecyclingBinItemRenderer implements SpecialModelRenderer<Integer> {

    @SuppressWarnings("unchecked")
    private static final StandaloneModelKey<BlockStateModel>[] PART_MODELS = new StandaloneModelKey[] {
        RecyclingBinRenderer.BODY_MODEL_KEY, RecyclingBinRenderer.LID_MODEL_KEY, RecyclingBinRenderer.PEDAL_MODEL_KEY
    };

    @Override
    public void getExtents(Consumer<Vector3fc> output) {}

    @Override
    @Nullable
    public Integer extractArgument(ItemStack stack) {
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.getUnsafe();
        return tag.getInt("Vibes").orElse(0);
    }

    @Override
    public void submit(
            @Nullable Integer vibes,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int packedOverlay,
            boolean hasFoil,
            int tint) {
        Minecraft mc = Minecraft.getInstance();
        var random = RandomSource.create();

        for (StandaloneModelKey<BlockStateModel> key : PART_MODELS) {
            BlockStateModel model = mc.getModelManager().getStandaloneModel(key);
            List<BlockModelPart> parts = new ArrayList<>();
            random.setSeed(42L);
            model.collectParts(random, parts);
            final List<BlockModelPart> fparts = parts;
            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, vc) -> {
                for (BlockModelPart part : fparts) {
                    for (Direction dir : Direction.values()) {
                        for (var quad : part.getQuads(dir)) {
                            vc.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
                        }
                    }
                    for (var quad : part.getQuads(null)) {
                        vc.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
                    }
                }
            });
        }

        if (vibes == null || vibes <= 0) return;

        float fillFraction = (float) vibes / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
        float fill = Math.max(0.01f, fillFraction);
        float fillTop = RecyclingBinRenderer.FLUID_FLOOR + fill * RecyclingBinRenderer.FLUID_H;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(Registration.POSITIVE_VIBES_TYPE.get());
        TextureAtlasSprite sprite = mc.getAtlasManager()
                .getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ext.getStillTexture());
        int fluidTint = ext.getTintColor();
        int fr = (fluidTint >> 16) & 0xFF;
        int fg = (fluidTint >> 8) & 0xFF;
        int fb = fluidTint & 0xFF;
        int fa = (fluidTint >> 24) & 0xFF;
        if (fa == 0) fa = 77;
        int fluidLight =
                Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;
        float uL = sprite.getU0(), uR = sprite.getU1();
        float vT = sprite.getV0(), vB = Mth.lerp(fill, sprite.getV0(), sprite.getV1());

        final int ffrF = fr, ffgF = fg, ffbF = fb, ffaF = fa, flF = fluidLight, ovF = packedOverlay;
        collector.submitCustomGeometry(
                poseStack,
                Sheets.translucentItemSheet(),
                (pose, vc) -> RecyclingBinRenderer.renderFluidGeometry(
                        vc, pose.pose(), ffrF, ffgF, ffbF, ffaF, flF, ovF, uL, uR, vT, vB, fillTop));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new RecyclingBinItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
