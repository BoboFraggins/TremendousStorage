package net.bobofraggins.intellistore.storage.battery;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.intellistore.shared.tank.AbstractTankRenderer;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders the Battery's dynamic content: energy fill level and tube-connector stubs.
 *
 * <p>The fill is drawn as a neon-green-tinted flowing-water animation to suggest electrical energy.
 * The static shell is rendered by the block model ({@code models/block/battery.json}).
 */
public class BatteryRenderer extends AbstractTankRenderer<BatteryBlockEntity> {

    // Vanilla flowing-water animated sprite — its white/gray pixels tint cleanly
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    // Neon green, slightly transparent
    private static final int FILL_R = 57;
    private static final int FILL_G = 255;
    private static final int FILL_B = 20;
    private static final int FILL_A = 200;

    private static final float MIN_FILL_FRAC = 0.05f;

    public BatteryRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected void renderFill(
            BatteryBlockEntity be, Matrix4f mat, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (be.getEnergyStored() <= 0) return;

        float fillFrac = Math.max(MIN_FILL_FRAC, (float) be.getEnergyStored() / be.getMaxEnergy());
        float fillTop = FLUID_FLOOR + fillFrac * FLUID_H;

        var fillSprite = sprite(WATER_FLOW);
        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        float uLeft = fillSprite.getU0();
        float uRight = fillSprite.getU1();
        float vTop = fillSprite.getV0();
        float vBottom = Mth.lerp(fillFrac, fillSprite.getV0(), fillSprite.getV1());

        FluidTankRenderer.renderOctagonalPrism(
                vc,
                mat,
                FILL_R,
                FILL_G,
                FILL_B,
                FILL_A,
                LightTexture.FULL_BRIGHT,
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
