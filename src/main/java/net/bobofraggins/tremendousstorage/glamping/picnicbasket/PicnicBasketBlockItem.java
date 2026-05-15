package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.StorageTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Block item for the Picnic Basket (regular and Ender). Adds a tooltip describing auto-feed. */
public class PicnicBasketBlockItem extends TieredBlockItem {

    private final String tooltipKey;

    public PicnicBasketBlockItem(Block block, String tooltipKey) {
        super(block, new Properties());
        this.tooltipKey = tooltipKey;
    }

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
