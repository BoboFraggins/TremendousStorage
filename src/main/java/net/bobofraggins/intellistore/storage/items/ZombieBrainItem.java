package net.bobofraggins.intellistore.storage.items;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A brain harvested from a zombie. 1-in-8 chance drop from any Zombie subtype. */
public class ZombieBrainItem extends Item {
    public ZombieBrainItem() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.intellistore.zombie_brain.tooltip"));
    }
}
