package net.bobofraggins.intellistore.junkdrawer;

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
 * Jade (WAILA) plugin for the Junk Drawer.
 *
 * <p>Shows the current item count and total capacity, e.g. "128 / 32k items", or "Empty".
 */
@WailaPlugin
public class JunkDrawerJadePlugin implements IWailaPlugin {

    static final ResourceLocation JUNK_DRAWER_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "junk_drawer");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DrawerDataProvider.INSTANCE, JunkDrawerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DrawerDataProvider.INSTANCE, JunkDrawerBlock.class);
    }

    enum DrawerDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_TOTAL = "Total";
        private static final String KEY_CAPACITY = "Capacity";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof JunkDrawerBlockEntity be)) return;
            data.putInt(KEY_TOTAL, be.size());
            data.putInt(KEY_CAPACITY, JunkDrawerBlockEntity.CAPACITY);
        }

        @Override
        public ResourceLocation getUid() {
            return JUNK_DRAWER_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            int total = data.getInt(KEY_TOTAL);

            if (total == 0) {
                tooltip.add(Component.translatable("jade.intellistore.junk_drawer.empty"));
                return;
            }

            tooltip.add(Component.translatable(
                    "jade.intellistore.junk_drawer.total",
                    CountFormat.format(total),
                    CountFormat.format(data.getInt(KEY_CAPACITY))));
        }
    }
}
