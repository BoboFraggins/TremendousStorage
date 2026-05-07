package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        boolean ender = data.getBoolean("Ender");
        boolean crafting = data.getBoolean("CraftingUpgrade");
        boolean magnet = data.getBoolean("MagnetUpgrade");
        boolean puller = data.getBoolean("PullerUpgrade");
        boolean haarp = data.getBoolean("HaarpUpgrade");
        boolean interdimensional = data.getBoolean("InterdimensionalUpgrade");
        boolean compacting = data.getBoolean("CompactingUpgrade");

        if (tier == StorageTier.WOOD
                && !ender
                && !crafting
                && !magnet
                && !puller
                && !haarp
                && !interdimensional
                && !compacting) return;

        StringBuilder sb = new StringBuilder();
        if (tier != StorageTier.WOOD) sb.append(TieredBlockItem.capitalize(tier.getId()));
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

        Component baseName = Component.translatable(accessor.getBlock().getDescriptionId());
        tooltip.remove(JadeIds.MC_BLOCK_DISPLAY);
        tooltip.add(0, Component.empty().append(baseName).append(" (" + sb + ")"));
    }
}
