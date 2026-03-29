package net.bobofraggins.intellistore.external.mekanism;

import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.api.chemical.ChemicalStack;
import net.bobofraggins.intellistore.shared.tank.AbstractTankRenderer;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders the Gas Tank's dynamic content: chemical fill level and tube-connector stubs.
 *
 * <p>The static shell is rendered by the block model ({@code models/block/gas_tank.json}).
 * This BESR handles the translucent octagonal-prism fill and lazurite connector stubs.
 */
public class GasTankRenderer extends AbstractTankRenderer<GasTankBlockEntity> {

    private static final ResourceLocation FLUID_TANK =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/fluid_tank");

    private static final float MIN_FILL_FRAC = 0.05f;

    public GasTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected void renderFill(
            GasTankBlockEntity be, Matrix4f mat, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (!be.isLocked()) return;

        ChemicalStack chemical = be.getStoredChemical();

        float fillFrac =
                be.getAmount() > 0 ? Math.max(MIN_FILL_FRAC, (float) be.getAmount() / be.getCapacity()) : MIN_FILL_FRAC;
        float fillTop = FLUID_FLOOR + fillFrac * FLUID_H;

        int tint = chemical.getChemicalTint();
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 255;

        var fillSprite = sprite(FLUID_TANK);
        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        float uLeft = fillSprite.getU0();
        float uRight = fillSprite.getU1();
        float vTop = fillSprite.getV0();
        float vBottom = Mth.lerp(fillFrac, fillSprite.getV0(), fillSprite.getV1());

        FluidTankRenderer.renderOctagonalPrism(
                vc,
                mat,
                fr,
                fg,
                fb,
                fa,
                packedLight,
                packedOverlay,
                uLeft,
                vTop,
                uRight,
                vBottom,
                fillSprite.getU0(),
                fillSprite.getU1(),
                fillSprite.getV0(),
                fillSprite.getV1(),
                fillTop);
    }
}
