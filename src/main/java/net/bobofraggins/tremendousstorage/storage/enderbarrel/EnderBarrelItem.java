package net.bobofraggins.tremendousstorage.storage.enderbarrel;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.storage.EnderTieredBlockItem;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class EnderBarrelItem extends EnderTieredBlockItem {

    public EnderBarrelItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        BarrelItem.appendBarrelContents(stack, lines);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        String locked = BarrelItem.buildLockedSuffix(stack);
        return locked.isEmpty() ? base : Component.empty().append(base).append(locked);
    }
}
