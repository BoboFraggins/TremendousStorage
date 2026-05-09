package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Appends the storage tier and upgrade names to the block display name in the Jade HUD.
 * Example: "Tremendous Chest (Diamond/Crafting/Puller)".
 */
public enum StorageTierJadeComponentProvider implements IComponentProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return StorageTierJadeDataProvider.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        StorageTier tier =
                data.contains("StorageTier") ? StorageTier.fromId(data.getString("StorageTier")) : StorageTier.WOOD;
        boolean crafting = data.getBoolean("CraftingUpgrade");
        boolean magnet = data.getBoolean("MagnetUpgrade");
        boolean puller = data.getBoolean("PullerUpgrade");
        boolean haarp = data.getBoolean("HaarpUpgrade");
        boolean interdimensional = data.getBoolean("InterdimensionalUpgrade");
        boolean compacting = data.getBoolean("CompactingUpgrade");

        boolean hasUpgrades =
                tier != StorageTier.WOOD || crafting || magnet || puller || haarp || interdimensional || compacting;

        if (hasUpgrades) {
            StringBuilder sb = new StringBuilder();
            if (tier != StorageTier.WOOD) sb.append(TieredBlockItem.capitalize(tier.getId()));
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
            Component baseName = Component.translatable(accessor.getBlock().getDescriptionId());
            tooltip.remove(JadeIds.MC_BLOCK_DISPLAY);
            tooltip.add(0, Component.empty().append(baseName).append(" (" + sb + ")"));
        }

        if (accessor.getBlockEntity() instanceof BarrelBlockEntity barrel) {
            tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);
            if (barrel.isLocked()) {
                if (barrel.hasCompactingUpgrade()) {
                    appendCompactingLines(tooltip, barrel);
                } else {
                    tooltip.add(contentLine(barrel.getStoredItem(), barrel.getCount(), barrel.getCapacity()));
                }
            }
        }
    }

    private static void appendCompactingLines(ITooltip tooltip, BarrelBlockEntity barrel) {
        int base = barrel.getBaseSlot();
        long raw = barrel.getCount();
        long cap = barrel.getCapacity();

        if (base == 0) {
            tooltip.add(contentLine(barrel.getStoredItem(), raw, cap));
        }

        // Slot 1
        ItemStack s1;
        long c1, m1;
        if (base == 0 && !barrel.getCompactTier1Item().isEmpty() && barrel.getCompactTier1Ratio() > 0) {
            s1 = barrel.getCompactTier1Item();
            c1 = raw / barrel.getCompactTier1Ratio();
            m1 = cap / barrel.getCompactTier1Ratio();
        } else if (base == 1) {
            s1 = barrel.getStoredItem();
            c1 = raw;
            m1 = cap;
        } else {
            return;
        }
        tooltip.add(contentLine(s1, c1, m1));

        // Slot 2
        ItemStack s2;
        long c2, m2;
        if (base == 0
                && !barrel.getCompactTier2Item().isEmpty()
                && barrel.getCompactTier1Ratio() > 0
                && barrel.getCompactTier2Ratio() > 0) {
            long combined = (long) barrel.getCompactTier1Ratio() * barrel.getCompactTier2Ratio();
            s2 = barrel.getCompactTier2Item();
            c2 = raw / combined;
            m2 = cap / combined;
        } else if (base == 1 && !barrel.getCompactTier1Item().isEmpty() && barrel.getCompactTier1Ratio() > 0) {
            s2 = barrel.getCompactTier1Item();
            c2 = raw / barrel.getCompactTier1Ratio();
            m2 = cap / barrel.getCompactTier1Ratio();
        } else if (base == 2) {
            s2 = barrel.getStoredItem();
            c2 = raw;
            m2 = cap;
        } else {
            return;
        }
        tooltip.add(contentLine(s2, c2, m2));
    }

    private static Component contentLine(ItemStack item, long count, long max) {
        return Component.literal(CountFormat.format(count) + "/" + CountFormat.format(max) + " "
                + item.getHoverName().getString());
    }
}
