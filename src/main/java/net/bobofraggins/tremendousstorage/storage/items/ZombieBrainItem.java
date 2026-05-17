package net.bobofraggins.tremendousstorage.storage.items;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A brain harvested from a zombie. 1-in-8 chance drop from any Zombie subtype. */
public class ZombieBrainItem extends Item {
    public ZombieBrainItem() {
        super(new Item.Properties());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext ctx,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.tremendousstorage.zombie_brain.tooltip"));
    }
}
