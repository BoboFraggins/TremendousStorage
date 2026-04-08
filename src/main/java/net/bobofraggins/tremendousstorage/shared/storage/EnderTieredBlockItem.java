package net.bobofraggins.tremendousstorage.shared.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * A {@link BlockItem} for Ender-variant storage blocks that appends an "(Ender)" or
 * "(Diamond/Ender)" suffix to the item display name.
 *
 * <p>Wood-tier: "Tremendous Chest (Ender)"
 * <p>Other tiers: "Tremendous Chest (Diamond/Ender)" etc.
 */
public class EnderTieredBlockItem extends BlockItem {

    public EnderTieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack); // uses the block's translation key
        StorageTier tier = tierFromStack(stack);
        if (tier == StorageTier.WOOD) {
            return Component.empty().append(base).append(" (Ender)");
        }
        return Component.empty().append(base).append(" (" + capitalize(tier.getId()) + "/Ender)");
    }

    private static StorageTier tierFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return StorageTier.WOOD;
        CompoundTag tag = data.copyTag();
        return StorageTier.fromId(tag.getString("Tier"));
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
