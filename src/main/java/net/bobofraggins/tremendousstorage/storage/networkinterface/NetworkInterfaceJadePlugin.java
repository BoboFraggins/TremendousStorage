package net.bobofraggins.tremendousstorage.storage.networkinterface;

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
 * Jade (WAILA) plugin for the Network Interface.
 *
 * <p>Appends the network validity status.
 */
@WailaPlugin
public class NetworkInterfaceJadePlugin implements IWailaPlugin {

    static final ResourceLocation NI_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "network_interface_status");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(NiDataProvider.INSTANCE, NetworkInterfaceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(NiDataProvider.INSTANCE, NetworkInterfaceBlock.class);
    }

    enum NiDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_VALID = "NetworkValid";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof NetworkInterfaceBlockEntity be)) return;
            data.putBoolean(KEY_VALID, be.isNetworkValid());
        }

        @Override
        public ResourceLocation getUid() {
            return NI_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            boolean valid = data.getBoolean(KEY_VALID);
            if (!valid) {
                tooltip.add(Component.translatable("jade.tremendousstorage.network_interface.invalid")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }
}
