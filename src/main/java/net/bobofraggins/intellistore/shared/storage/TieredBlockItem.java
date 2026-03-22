package net.bobofraggins.intellistore.shared.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * A {@link BlockItem} that appends the storage tier name to the item's display name for any tier
 * above {@link StorageTier#PAPER}.
 *
 * <p>The tier is read from the item stack's {@code minecraft:block_entity_data} component (saved
 * there when the block is broken via {@code BlockEntity.saveToItem}).
 *
 * <p>Example: a Diamond-tier Bulk Storage Container shows as "Bulk Storage Container (Diamond)".
 * A Paper-tier block shows the unmodified translation.
 */
public class TieredBlockItem extends BlockItem {

    public TieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        StorageTier tier = tierFromStack(stack);
        if (tier == StorageTier.PAPER) return base;
        String suffix = " (" + capitalize(tier.getId()) + ")";
        return Component.empty().append(base).append(suffix);
    }

    private static StorageTier tierFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return StorageTier.PAPER;
        CompoundTag tag = data.copyTag();
        return StorageTier.fromId(tag.getString("Tier"));
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
