package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    public Identifier getUid() {
        return StorageTierJadeDataProvider.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        StorageTier tier = data.contains("StorageTier")
                ? StorageTier.fromId(data.getStringOr("StorageTier", ""))
                : StorageTier.WOOD;
        boolean crafting = data.getBooleanOr("CraftingUpgrade", false);
        boolean magnet = data.getBooleanOr("MagnetUpgrade", false);
        boolean puller = data.getBooleanOr("PullerUpgrade", false);
        boolean haarp = data.getBooleanOr("HaarpUpgrade", false);
        boolean interdimensional = data.getBooleanOr("InterdimensionalUpgrade", false);
        boolean compacting = data.getBooleanOr("CompactingUpgrade", false);

        boolean hasUpgrades =
                tier != StorageTier.WOOD || crafting || magnet || puller || haarp || interdimensional || compacting;

        if (hasUpgrades) {
            String suffix = new net.bobofraggins.tremendousstorage.shared.util.UpgradeSuffix()
                    .tier(tier)
                    .addIf(crafting, "upgrade.tremendousstorage.crafting")
                    .addIf(magnet, "upgrade.tremendousstorage.magnet")
                    .addIf(puller, "upgrade.tremendousstorage.puller")
                    .addIf(haarp, "upgrade.tremendousstorage.haarp")
                    .addIf(interdimensional, "upgrade.tremendousstorage.interdimensional")
                    .addIf(compacting, "upgrade.tremendousstorage.compacting")
                    .toString();
            Component baseName = Component.translatable(accessor.getBlock().getDescriptionId());
            tooltip.remove(JadeIds.MC_BLOCK_DISPLAY);
            tooltip.add(0, Component.empty().append(baseName).append(suffix));
        }
    }
}
