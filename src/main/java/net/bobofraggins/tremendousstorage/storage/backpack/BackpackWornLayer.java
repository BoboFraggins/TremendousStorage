package net.bobofraggins.tremendousstorage.storage.backpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Render layer that draws the Tremendous Backpack on the player's back when the
 * backpack is equipped in a Curios "back" slot or carried in the inventory.
 *
 * <p>Uses {@link net.minecraft.client.renderer.entity.ItemRenderer#renderStatic} so the item
 * renderer handles render-type selection and texture binding correctly within the entity
 * rendering pipeline. Direct BakedModel + RenderType.solid() access does not flush in time
 * during entity rendering and produces no visible output.
 *
 * <p>Transform chain (applied in order after {@code body.translateAndRotate}):
 * <ol>
 *   <li>YP 180° — turns the model so its front faces outward (+Z = player's back).
 *   <li>ZP 180° — corrects vertical orientation, mirroring the entity renderer's implicit
 *       Y-flip so +Y is up in world space.
 *   <li>translate(0, {@value #Y_OFFSET}, {@value #Z_OFFSET}) — centres vertically on the
 *       body and embeds the straps against the player's back surface.
 * </ol>
 */
public class BackpackWornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Vertical offset (blocks) relative to body pivot — negative = toward feet. */
    private static final float Y_OFFSET = -0.25f;

    /** Depth offset (blocks) — negative pushes the straps into the player's back. */
    private static final float Z_OFFSET = -0.3f;

    public BackpackWornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        ItemStack backpackStack = findBackpack(player);
        if (backpackStack.isEmpty()) return;

        poseStack.pushPose();

        // Anchor to the body part so the backpack rotates with the torso.
        getParentModel().body.translateAndRotate(poseStack);

        // Orient the model: YP-180 faces it outward, ZP-180 corrects the Y-axis.
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));

        poseStack.translate(0f, Y_OFFSET, Z_OFFSET);

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        backpackStack,
                        ItemDisplayContext.NONE,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffers,
                        player.level(),
                        0);

        poseStack.popPose();
    }

    // -------------------------------------------------------------------------
    // Backpack detection
    // -------------------------------------------------------------------------

    private static ItemStack findBackpack(AbstractClientPlayer player) {
        // Check Curios "back" slot first (soft-optional).
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv != null) {
                var entry = inv.getCurios().get("back");
                if (entry != null) {
                    for (int i = 0; i < entry.getStacks().getSlots(); i++) {
                        ItemStack s = entry.getStacks().getStackInSlot(i);
                        if (s.getItem() instanceof BackpackItem) return s;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        // Fall back to scanning main inventory.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof BackpackItem) return s;
        }
        return ItemStack.EMPTY;
    }
}
