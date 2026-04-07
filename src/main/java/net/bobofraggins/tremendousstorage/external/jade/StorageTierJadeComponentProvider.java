package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Appends the storage tier name to the block display name in the Jade HUD for any tier above
 * {@link StorageTier#WOOD}. Example: "Bulk Storage Container (Diamond)".
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
        if (!data.contains("StorageTier")) return;
        StorageTier tier = StorageTier.fromId(data.getString("StorageTier"));
        if (tier == StorageTier.WOOD) return;

        String tierName =
                Character.toUpperCase(tier.getId().charAt(0)) + tier.getId().substring(1);
        Component baseName = Component.translatable(accessor.getBlock().getDescriptionId());
        tooltip.remove(JadeIds.MC_BLOCK_DISPLAY);
        tooltip.add(0, Component.empty().append(baseName).append(" (" + tierName + ")"));
    }
}
