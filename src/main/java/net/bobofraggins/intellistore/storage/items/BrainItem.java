package net.bobofraggins.intellistore.storage.items;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A healed brain, created by dipping a Zombie Brain into a Healing Salve cauldron. */
public class BrainItem extends Item {
    public BrainItem() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.intellistore.brain.tooltip"));
    }
}
