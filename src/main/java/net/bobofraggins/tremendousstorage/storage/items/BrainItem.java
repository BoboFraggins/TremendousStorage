package net.bobofraggins.tremendousstorage.storage.items;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A healed brain, created by dipping a Zombie Brain into a Positive Vibes cauldron. */
public class BrainItem extends Item {
    public BrainItem() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext ctx,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.tremendousstorage.brain.tooltip"));
    }
}
