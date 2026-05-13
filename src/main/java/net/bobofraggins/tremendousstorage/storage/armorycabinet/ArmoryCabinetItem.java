package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.util.StorageTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class ArmoryCabinetItem extends BlockItem {

    public ArmoryCabinetItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        StorageTooltip.appendBlockEntityItems(stack, lines, context);
    }
}
