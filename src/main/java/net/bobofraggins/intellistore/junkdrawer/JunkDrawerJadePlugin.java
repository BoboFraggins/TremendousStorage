package net.bobofraggins.intellistore.junkdrawer;

import net.bobofraggins.intellistore.filingcabinet.FilingCabinetJadePlugin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
 * <p>Shows the stored item and its count/capacity, e.g. "4k of 32k Cobblestone".
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

        private static final String KEY_COUNT = "Count";
        private static final String KEY_CAPACITY = "Capacity";
        private static final String KEY_STACK = "Stack";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof JunkDrawerBlockEntity be)) return;
            data.putLong(KEY_COUNT, be.getCount());
            data.putLong(KEY_CAPACITY, JunkDrawerBlockEntity.CAPACITY);
            ItemStack type = be.getStoredType();
            if (!type.isEmpty()) {
                data.put(KEY_STACK, type.save(accessor.getLevel().registryAccess()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return JUNK_DRAWER_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();

            if (!data.contains(KEY_STACK)) {
                tooltip.add(Component.translatable("jade.intellistore.junk_drawer.empty"));
                return;
            }

            ItemStack stack =
                    ItemStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound(KEY_STACK));
            if (stack.isEmpty()) return;

            long count = data.getLong(KEY_COUNT);
            long capacity = data.getLong(KEY_CAPACITY);
            tooltip.add(Component.translatable(
                    "jade.intellistore.junk_drawer.contents",
                    FilingCabinetJadePlugin.formatCount(count),
                    FilingCabinetJadePlugin.formatCount(capacity),
                    stack.getHoverName()));
        }
    }
}
