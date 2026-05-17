package net.bobofraggins.tremendousstorage.storage.enderchest;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.storage.EnderTieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.StorageTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class EnderChestItem extends EnderTieredBlockItem {

    public EnderChestItem(Block block, Properties properties) {
        super(block, properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, tooltipAdder, flag);
        StorageTooltip.appendBlockEntityItems(stack, tooltipAdder, context);
    }
}
