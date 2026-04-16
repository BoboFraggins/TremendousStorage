package net.bobofraggins.tremendousstorage.storage.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.tank.AbstractTankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Custom item renderer for Tank and Ender Tank.
 *
 * <p>Renders the static glass-jar block model and, if the item has stored fluid,
 * overlays the octagonal-prism fill using the fluid's actual still texture and tint colour.
 */
public class TankItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static TankItemRenderer instance;

    public TankItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    /** Lazily constructed singleton — must be called from the render thread. */
    public static TankItemRenderer getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new TankItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
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

        // Render the glass jar model (pink placeholder pixels are now transparent).
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
                .renderSingleBlock(
                        Registration.TANK.get().defaultBlockState(),
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay);

        // Render fluid fill only when the item actually contains fluid.
        TankContents contents = stack.get(Registration.TANK_CONTENTS.get());
        if (contents == null || contents.isEmpty()) return;

        // Derive capacity from the stored tier (falls back to WOOD if absent).
        StorageTier tier = StorageTier.WOOD;
        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.contains("Tier")) {
                tier = StorageTier.fromId(tag.getString("Tier"));
            }
        }
        long capacity = tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
        float fillFrac = Math.max(0.01f, (float) contents.amount() / capacity);

        FluidStack fluid = contents.storedFluid();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());

        TextureAtlasSprite sprite = mc.getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(ext.getStillTexture(fluid));

        int tint = ext.getTintColor(fluid);
        int fr = (tint >> 16) & 0xFF;
        int fg = (tint >> 8) & 0xFF;
        int fb = tint & 0xFF;
        int fa = (tint >> 24) & 0xFF;
        if (fa == 0) fa = 77;

        int fluidLight = fluid.getFluidType().getLightLevel() > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        float fillTop = AbstractTankRenderer.FLUID_FLOOR + fillFrac * AbstractTankRenderer.FLUID_H;
        float uLeft = sprite.getU0();
        float uRight = sprite.getU1();
        float vTop = sprite.getV0();
        float vBottom = Mth.lerp(fillFrac, sprite.getV0(), sprite.getV1());

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        TankRenderer.renderOctagonalPrism(
                vc,
                poseStack.last().pose(),
                fr, fg, fb, fa,
                fluidLight,
                packedOverlay,
                uLeft, vTop, uRight, vBottom,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
                fillTop);
    }
}
