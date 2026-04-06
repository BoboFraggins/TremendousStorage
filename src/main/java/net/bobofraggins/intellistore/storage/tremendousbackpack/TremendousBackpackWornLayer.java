package net.bobofraggins.intellistore.storage.tremendousbackpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Render layer that draws the Tremendous Backpack model on the player's back
 * when the backpack is equipped in a Curios "back" slot or carried in the inventory.
 *
 * <p>Transform derivation (body-pivot-relative space, after undoing the entity
 * rendering's scale(-1,-1,1) flip):
 * <ul>
 *   <li>+Y = up, +Z = player's back direction, +X = player's right</li>
 *   <li>Rotate 180° around the model's geometric centre (0.5, 0.375, 0.5 blocks)
 *       so the clasp (low-Z in model) faces outward and the straps (high-Z in model)
 *       face the player's back.</li>
 *   <li>Translate to center the model on the body and embed the straps slightly
 *       into the player's back.</li>
 * </ul>
 *
 * <p>Visual positioning may need tuning depending on the exact model geometry.
 * Adjust {@link #Z_EMBED} to change how deeply the straps sit inside the player's back.
 */
public class TremendousBackpackWornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * How far (in blocks) the straps are pushed into the player's back.
     * Positive = further embedded; negative = floating away from body.
     */
    private static final float Z_EMBED = 0.15f;

    public TremendousBackpackWornLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
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

        Minecraft mc = Minecraft.getInstance();
        BakedModel bodyModel = mc.getModelManager().getModel(TremendousBackpackRenderer.BODY_MODEL);
        BakedModel flapModel = mc.getModelManager().getModel(TremendousBackpackRenderer.FLAP_MODEL);

        poseStack.pushPose();

        // Position relative to the player's body part so the backpack rotates with the body.
        getParentModel().body.translateAndRotate(poseStack);

        // Undo the entity renderer's scale(-1,-1,1) flip so we work with natural axes.
        // After this: +Y = up, +Z = player's back direction.
        poseStack.scale(-1.0f, -1.0f, 1.0f);

        // Rotate 180° around the model's geometric centre to orient the clasp outward.
        // Block model extents: x=[4..12], y=[0..12], z=[4.75..12.75] (in 1/16-block units)
        // Model centre: x=0.5 blocks, y=0.375 blocks, z=0.5 blocks
        // After YP-180 at centre: clasp (z≈5/16→11/16) faces +Z (player's back); straps (z≈12.5/16→3.5/16) face body.
        poseStack.translate(-0.5f, -0.375f, -0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.translate(0.5f, 0.375f, 0.5f);

        // Final placement: centre on body (x), align model bottom with body bottom (y),
        // and push straps into the player's back by Z_EMBED blocks.
        // x: model x-centre (0.5) → player centre (0) → shift -0.5
        // y: model y=0 → body bottom → shift -0.375 (= -6/16; body extends ±6 px from pivot)
        // z: straps end up at ~0.219 blocks from pivot; body back face = 0.125 blocks → shift by -(0.219 - 0.125 +
        // Z_EMBED)
        float zShift = -(0.219f - 0.125f + Z_EMBED);
        poseStack.translate(-0.5f, -0.375f, zShift);

        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create();
        renderModel(consumer, poseStack.last(), bodyModel, packedLight, random);
        renderModel(consumer, poseStack.last(), flapModel, packedLight, random);

        poseStack.popPose();
    }

    private static void renderModel(
            VertexConsumer consumer, PoseStack.Pose pose, BakedModel model, int packedLight, RandomSource random) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            random.setSeed(42L);
            for (var quad : model.getQuads(null, dir, random, ModelData.EMPTY, RenderType.solid())) {
                float shade = 1.0f;
                consumer.putBulkData(
                        pose,
                        quad,
                        shade,
                        shade,
                        shade,
                        1.0f,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            }
        }
        random.setSeed(42L);
        for (var quad : model.getQuads(null, null, random, ModelData.EMPTY, RenderType.solid())) {
            consumer.putBulkData(
                    pose,
                    quad,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f,
                    packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }
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
                        if (s.getItem() instanceof TremendousBackpackItem) return s;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        // Fall back to scanning main inventory.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof TremendousBackpackItem) return s;
        }
        return ItemStack.EMPTY;
    }
}
