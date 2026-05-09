package net.bobofraggins.tremendousstorage.shared.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A {@link TieredBlockItem} for Ender-variant storage blocks whose base translation already
 * includes "Ender" (e.g. "Ender Barrel"), so the upgrade suffix omits the redundant word.
 */
public class EnderTieredBlockItem extends TieredBlockItem {

    public EnderTieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = Component.translatable(getDescriptionId());
        String suffix = buildBedSuffix(stack, false);
        return suffix.isEmpty() ? base : Component.empty().append(base).append(suffix);
    }
}
