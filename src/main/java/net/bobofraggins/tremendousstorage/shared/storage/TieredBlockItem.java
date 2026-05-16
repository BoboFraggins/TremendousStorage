package net.bobofraggins.tremendousstorage.shared.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A {@link BlockItem} that appends the storage tier and any applied upgrades to the item's
 * display name whenever at least one upgrade is present.
 *
 * <p>The tier and upgrade flags are read from the item stack's {@code minecraft:block_entity_data}
 * component (saved there when the block is broken via {@code BlockEntity.saveToItem}).
 *
 * <p>Example: a Diamond-tier chest with Crafting upgrade shows as
 * "Tremendous Chest (Diamond/Crafting)".
 */
public class TieredBlockItem extends BlockItem {

    public TieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        String suffix = buildBedSuffix(stack, false);
        return suffix.isEmpty() ? base : Component.empty().append(base).append(suffix);
    }

    /**
     * Builds the upgrade suffix (e.g. {@code " (Diamond/Crafting/Magnet)"}) from the stack's
     * {@code block_entity_data} component. Pass {@code ender=true} for Ender-variant items to
     * inject "Ender" after the tier in the suffix.
     *
     * <p>Returns an empty string when no upgrades are present (Wood tier, no flags set).
     */
    public static String buildBedSuffix(ItemStack stack, boolean ender) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        StorageTier tier = StorageTier.WOOD;
        boolean crafting = false,
                magnet = false,
                puller = false,
                haarp = false,
                interdimensional = false,
                compacting = false;
        if (data != null) {
            CompoundTag tag = data.getUnsafe();
            tier = StorageTier.fromId(tag.getStringOr("Tier", ""));
            crafting = tag.getBooleanOr("CraftingUpgrade", false);
            magnet = tag.getBooleanOr("MagnetUpgrade", false);
            puller = tag.getBooleanOr("PullerUpgrade", false);
            haarp = tag.getBooleanOr("HaarpUpgrade", false);
            interdimensional = tag.getBooleanOr("InterdimensionalUpgrade", false);
            compacting = tag.getBooleanOr("CompactingUpgrade", false);
        }
        StringBuilder sb = new StringBuilder();
        if (tier != StorageTier.WOOD) sb.append(capitalize(tier.getId()));
        if (ender) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Ender");
        }
        if (crafting) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Crafting");
        }
        if (magnet) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Magnet");
        }
        if (puller) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Puller");
        }
        if (haarp) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Haarp");
        }
        if (interdimensional) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Interdimensional");
        }
        if (compacting) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Compacting");
        }
        return sb.isEmpty() ? "" : " (" + sb + ")";
    }

    public static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
