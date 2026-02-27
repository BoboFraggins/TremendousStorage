package net.bobofraggins.intellistore.bulkstorage;

import net.bobofraggins.intellistore.util.CountFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade (WAILA) plugin for the Bulk Storage Container.
 *
 * <p>Shows total item count and capacity, e.g. "4k / 32k items", or "Empty".
 */
@WailaPlugin
public class BulkStorageContainerJadePlugin implements IWailaPlugin {

    static final ResourceLocation BULK_STORAGE_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "bulk_storage_container");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ContainerDataProvider.INSTANCE, BulkStorageContainerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ContainerDataProvider.INSTANCE, BulkStorageContainerBlock.class);
    }

    enum ContainerDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_TOTAL = "Total";
        private static final String KEY_CAPACITY = "Capacity";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof BulkStorageContainerBlockEntity be)) return;
            data.putLong(KEY_TOTAL, be.totalCount());
            data.putLong(KEY_CAPACITY, BulkStorageContainerBlockEntity.CAPACITY);
        }

        @Override
        public ResourceLocation getUid() {
            return BULK_STORAGE_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            long total = data.getLong(KEY_TOTAL);

            if (total == 0) {
                tooltip.add(Component.translatable("jade.intellistore.bulk_storage_container.empty"));
                return;
            }

            tooltip.add(Component.translatable(
                    "jade.intellistore.bulk_storage_container.total",
                    CountFormat.format(total),
                    CountFormat.format(data.getLong(KEY_CAPACITY))));
        }
    }
}
