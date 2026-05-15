package net.bobofraggins.tremendousstorage.glamping.magichat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Render layer that draws the Magic Hat on the player's head when it is equipped in the
 * vanilla helmet slot. Uses the same pose transforms as {@link MagicHatCurioRenderer}.
 */
public class MagicHatHelmetLayer extends RenderLayer<PlayerRenderState, PlayerModel> {

    public MagicHatHelmetLayer(RenderLayerParent<PlayerRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            PlayerRenderState renderState,
            float yRot,
            float xRot) {

        // Retrieve the actual player entity to get the helmet stack
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof MagicHatItem)) return;

        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        poseStack.translate(0.0, 1.0, 0.0);

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        helmet,
                        ItemDisplayContext.NONE,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        player.level(),
                        0);

        poseStack.popPose();
    }
}
