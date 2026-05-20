package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.StorageTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Block item for the Picnic Basket (regular and Ender). Adds a tooltip describing auto-feed. */
public class PicnicBasketBlockItem extends TieredBlockItem {

    private final String tooltipKey;

    public PicnicBasketBlockItem(Block block, String tooltipKey, Item.Properties properties) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    // -------------------------------------------------------------------------
    // Right-click opens UI; sneak + right-click on a block face places the block
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return super.useOn(ctx);
        }
        if (!ctx.getLevel().isClientSide() && player instanceof ServerPlayer sp) {
            int slotIndex = ctx.getHand() == InteractionHand.MAIN_HAND
                    ? sp.getInventory().getSelectedSlot()
                    : 40;
            OpenPicnicBasketPacket.openUi(sp, PicnicBasketItemUtils.SLOT_INVENTORY, slotIndex, "");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int slotIndex =
                hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
        OpenPicnicBasketPacket.openUi((ServerPlayer) player, PicnicBasketItemUtils.SLOT_INVENTORY, slotIndex, "");
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltip,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable(tooltipKey));
        StorageTooltip.appendBlockEntityItems(stack, tooltipAdder, context);
    }
}
