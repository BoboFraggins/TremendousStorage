package net.bobofraggins.tremendousstorage.glamping.magichat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Renders the Magic Hat on the player's head when equipped in a Curios "head" slot. */
public class MagicHatCurioRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource bufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        if (stack.isEmpty()) return;
        EntityModel<T> model = renderLayerParent.getModel();
        if (!(model instanceof HumanoidModel<?> humanoid)) return;

        poseStack.pushPose();
        humanoid.head.translateAndRotate(poseStack);
        // Center the block model origin on the head and sit it on top:
        // block [8,0,8] (center-bottom) → head-local [0, top-of-head, 0]
        poseStack.translate(-0.5, 0.5, -0.5);

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        stack,
                        ItemDisplayContext.NONE,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        slotContext.entity().level(),
                        0);

        poseStack.popPose();
    }
}
