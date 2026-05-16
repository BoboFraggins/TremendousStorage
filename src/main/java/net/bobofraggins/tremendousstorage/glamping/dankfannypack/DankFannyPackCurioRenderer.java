package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Renders the Dank Fanny Pack on the player's waist when equipped in a Curios "belt" slot.
 */
public class DankFannyPackCurioRenderer implements ICurioRenderer {

    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            S renderState,
            RenderLayerParent<S, M> renderLayerParent,
            EntityRendererProvider.Context context,
            float yRotation,
            float xRotation) {

        if (stack.isEmpty()) return;
        M model = renderLayerParent.getModel();
        if (!(model instanceof HumanoidModel<?> humanoid)) return;

        poseStack.pushPose();
        humanoid.body.translateAndRotate(poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        poseStack.translate(0.0, -0.25, 0.0);

        ItemStackRenderState irs = new ItemStackRenderState();
        Minecraft.getInstance()
                .getItemModelResolver()
                .updateForTopItem(irs, stack, ItemDisplayContext.NONE, null, null, 0);
        irs.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
