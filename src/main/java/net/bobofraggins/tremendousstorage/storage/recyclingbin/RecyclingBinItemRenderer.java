package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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
        CompoundTag tag = customData.copyTagWithoutId();
        return tag.getInt("Vibes").orElse(0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void submit(
            @Nullable Integer vibes,
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
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(42L);
            model.collectParts(random, parts);
            final List<BlockStateModelPart> fparts = parts;
            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, vc) -> {
                for (BlockStateModelPart part : fparts) {
                    for (Direction dir : Direction.values()) {
                        for (var quad : part.getQuads(dir)) {
                            QuadInstance qi = new QuadInstance();
                            qi.setColor(0xFFFFFFFF);
                            qi.setLightCoords(packedLight);
                            qi.setOverlayCoords(packedOverlay);
                            vc.putBakedQuad(pose, quad, qi);
                        }
                    }
                    for (var quad : part.getQuads(null)) {
                        QuadInstance qi = new QuadInstance();
                        qi.setColor(0xFFFFFFFF);
                        qi.setLightCoords(packedLight);
                        qi.setOverlayCoords(packedOverlay);
                        vc.putBakedQuad(pose, quad, qi);
                    }
                }
            });
        }

        if (vibes == null || vibes <= 0) return;

        float fillFraction = (float) vibes / RecyclingBinBlockEntity.FLUID_CAPACITY_MB;
        float fill = Math.max(0.01f, fillFraction);
        float fillTop = RecyclingBinRenderer.FLUID_FLOOR + fill * RecyclingBinRenderer.FLUID_H;

        net.minecraft.client.renderer.block.FluidModel fluidModel_ = net.minecraft.client.Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(net.bobofraggins.tremendousstorage.shared.register.Registration.POSITIVE_VIBES_SOURCE
                        .get()
                        .defaultFluidState());
        TextureAtlasSprite sprite = fluidModel_.stillMaterial().sprite();
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
        if (fa == 0) fa = 77;
        int fluidLight = Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? 0xF000F0 : packedLight;
        float uL = sprite.getU0(), uR = sprite.getU1();
        float vT = sprite.getV0(), vB = Mth.lerp(fill, sprite.getV0(), sprite.getV1());

        final int ffrF = fr, ffgF = fg, ffbF = fb, ffaF = fa, flF = fluidLight, ovF = packedOverlay;
        collector.submitCustomGeometry(
                poseStack,
                Sheets.translucentBlockSheet(),
                (pose, vc) -> RecyclingBinRenderer.renderFluidGeometry(
                        vc, pose.pose(), ffrF, ffgF, ffbF, ffaF, flF, ovF, uL, uR, vT, vB, fillTop));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Integer> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        @Nullable
        public SpecialModelRenderer<Integer> bake(SpecialModelRenderer.BakingContext context) {
            return new RecyclingBinItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<Integer>> type() {
            return MAP_CODEC;
        }
    }
}
