package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Syncs the storage tier and upgrade flags from a tiered block entity to the client so
 * {@link StorageTierJadeComponentProvider} can append them to the block name in the Jade HUD.
 */
public enum StorageTierJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("tremendousstorage", "storage_tier");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof ChestBlockEntity chest) {
            data.putString("StorageTier", chest.getTier().getId());
            if (chest.hasCraftingUpgrade()) data.putBoolean("CraftingUpgrade", true);
            if (chest.hasMagnetUpgrade()) data.putBoolean("MagnetUpgrade", true);
            if (chest.hasPullerUpgrade()) data.putBoolean("PullerUpgrade", true);
        } else if (be instanceof TankBlockEntity tank) {
            data.putString("StorageTier", tank.getTier().getId());
            if (tank.hasMagnetUpgrade()) data.putBoolean("MagnetUpgrade", true);
            if (tank.hasPullerUpgrade()) data.putBoolean("PullerUpgrade", true);
        } else if (be instanceof FilingCabinetBlockEntity fc) {
            if (fc.hasMagnetUpgrade()) data.putBoolean("MagnetUpgrade", true);
            if (fc.hasPullerUpgrade()) data.putBoolean("PullerUpgrade", true);
        }
    }
}
