package net.bobofraggins.tremendousstorage.shared.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A {@link TieredBlockItem} for Ender-variant storage blocks that injects "Ender" into the
 * upgrade suffix after the tier, e.g. "(Ender)", "(Diamond/Ender)", "(Diamond/Ender/Crafting)".
 */
public class EnderTieredBlockItem extends TieredBlockItem {

    public EnderTieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = Component.translatable(getDescriptionId());
        String suffix = buildBedSuffix(stack, true);
        // Ender items always show at least "(Ender)"
        if (suffix.isEmpty()) suffix = " (Ender)";
        return Component.empty().append(base).append(suffix);
    }
}
