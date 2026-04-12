package net.bobofraggins.tremendousstorage.storage.chest;

import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
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
 * Jade (WAILA) plugin for the Tremendous Chest.
 *
 * <p>Shows total item count and capacity, e.g. "4k / 32k items", or "Empty".
 */
@WailaPlugin
public class ChestJadePlugin implements IWailaPlugin {

    static final ResourceLocation CHEST_PROVIDER = ResourceLocation.fromNamespaceAndPath("tremendousstorage", "chest");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ContainerDataProvider.INSTANCE, ChestBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ContainerDataProvider.INSTANCE, ChestBlock.class);
    }

    enum ContainerDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_TOTAL = "Total";
        private static final String KEY_CAPACITY = "Capacity";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ChestBlockEntity be)) return;
            data.putLong(KEY_TOTAL, be.totalCount());
            data.putLong(KEY_CAPACITY, be.getCapacity());
        }

        @Override
        public ResourceLocation getUid() {
            return CHEST_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            long total = data.getLong(KEY_TOTAL);

            if (total == 0) {
                tooltip.add(Component.translatable("jade.tremendousstorage.chest.empty"));
                return;
            }

            tooltip.add(Component.translatable(
                    "jade.tremendousstorage.chest.total",
                    CountFormat.format(total),
                    CountFormat.format(data.getLong(KEY_CAPACITY))));
        }
    }
}
