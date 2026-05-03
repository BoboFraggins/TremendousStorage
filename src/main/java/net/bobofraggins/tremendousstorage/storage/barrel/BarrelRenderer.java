package net.bobofraggins.tremendousstorage.storage.barrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the locked item and count on the barrel's front face.
 *
 * <p>The item is drawn flat (like an item frame) centred on the front face; the formatted
 * count appears as small 3D text immediately below it.
 */
public class BarrelRenderer implements BlockEntityRenderer<BarrelBlockEntity> {

    public BarrelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            BarrelBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {

        if (!be.isLocked()) return;
        ItemStack item = be.getStoredItem();
        if (item.isEmpty()) return;

        Direction facing = be.getBlockState().getValue(BarrelBlock.FACING);

        ps.pushPose();

        // Move to block centre, then align the local -Z axis with the barrel's front face.
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f));

        // Step just outside the front face (north face in local space = z = -0.5).
        ps.translate(0, 0, -0.502);

        // ── Item ──────────────────────────────────────────────────────────────
        ps.pushPose();
        ps.scale(0.5f, 0.5f, 0.001f);
        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(item, ItemDisplayContext.FIXED, light, overlay, ps, buffers, be.getLevel(), 0);
        ps.popPose();

        // ── Count label ───────────────────────────────────────────────────────
        String label = CountFormat.format(be.getCount());
        Font font = Minecraft.getInstance().font;
        float textScale = 1f / 80f;

        ps.pushPose();
        ps.translate(0, -0.28, 0);
        // Y is flipped so text doesn't appear upside-down in world space.
        ps.scale(textScale, -textScale, textScale);
        font.drawInBatch(
                label,
                -font.width(label) / 2f,
                0,
                0xFFFFFF,
                false,
                ps.last().pose(),
                buffers,
                Font.DisplayMode.NORMAL,
                0,
                light);
        ps.popPose();

        ps.popPose();
    }
}
