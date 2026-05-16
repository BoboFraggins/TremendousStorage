package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineBlockEntity;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Syncs the storage tier and upgrade flags from a tiered block entity to the client so
 * {@link StorageTierJadeComponentProvider} can append them to the block name in the Jade HUD.
 */
public enum StorageTierJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final Identifier UID = Identifier.fromNamespaceAndPath("tremendousstorage", "storage_tier");

    @Override
    public Identifier getUid() {
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
        } else if (be instanceof WirelessHubBlockEntity hub) {
            data.putString("StorageTier", hub.getTier().getId());
            if (hub.hasHaarpUpgrade()) data.putBoolean("HaarpUpgrade", true);
            if (hub.hasInterdimensionalUpgrade()) data.putBoolean("InterdimensionalUpgrade", true);
        } else if (be instanceof AccessTerminalBlockEntity at) {
            if (at.hasCraftingUpgrade()) data.putBoolean("CraftingUpgrade", true);
        } else if (be instanceof NetworkInterfaceBlockEntity ni) {
            data.putString("StorageTier", ni.getTier().getId());
        } else if (be instanceof BarrelBlockEntity barrel) {
            data.putString("StorageTier", barrel.getTier().getId());
            if (barrel.hasCompactingUpgrade()) data.putBoolean("CompactingUpgrade", true);
            if (barrel.hasPullerUpgrade()) data.putBoolean("PullerUpgrade", true);
        } else if (be instanceof StirlingEngineBlockEntity engine) {
            data.putString("StorageTier", engine.getTier().getId());
        }
    }
}
