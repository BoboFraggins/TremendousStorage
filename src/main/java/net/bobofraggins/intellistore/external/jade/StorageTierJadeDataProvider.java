package net.bobofraggins.intellistore.external.jade;

import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlockEntity;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Syncs the storage tier from a tiered block entity to the client so
 * {@link StorageTierJadeComponentProvider} can append it to the block name in the Jade HUD.
 */
public enum StorageTierJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("intellistore", "storage_tier");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        StorageTier tier = getTier(accessor.getBlockEntity());
        if (tier != null) {
            data.putString("StorageTier", tier.getId());
        }
    }

    static StorageTier getTier(BlockEntity be) {
        if (be instanceof BulkStorageContainerBlockEntity b) return b.getTier();
        if (be instanceof JunkDrawerBlockEntity j) return j.getTier();
        if (be instanceof FluidTankBlockEntity f) return f.getTier();
        return null;
    }
}
