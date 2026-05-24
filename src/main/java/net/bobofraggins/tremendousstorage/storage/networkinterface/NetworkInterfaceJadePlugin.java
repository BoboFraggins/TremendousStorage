package net.bobofraggins.tremendousstorage.storage.networkinterface;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * <p>Shows network power draw, active/inactive status, and a "Network Invalid" warning when
 * more than one Network Interface is present on the same network.
 */
@WailaPlugin
public class NetworkInterfaceJadePlugin implements IWailaPlugin {

    static final Identifier NI_PROVIDER =
            Identifier.fromNamespaceAndPath("tremendousstorage", "network_interface_status");

    private static final String KEY_VALID = "NetworkValid";
    private static final String KEY_POWERED = "Powered";
    private static final String KEY_FE_PER_TICK = "FePerTick";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(NiServerProvider.INSTANCE, NetworkInterfaceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(NiClientProvider.INSTANCE, NetworkInterfaceBlock.class);
    }

    enum NiServerProvider implements snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof NetworkInterfaceBlockEntity be)) return;
            data.putBoolean(KEY_VALID, be.isNetworkValid());
            data.putBoolean(KEY_POWERED, be.isPowered());
            NetworkScanResult scan = be.getScan();
            data.putInt(KEY_FE_PER_TICK, scan != null ? scan.totalFePerTick() : be.getBaseFePerTick());
        }

        @Override
        public Identifier getUid() {
            return NI_PROVIDER;
        }
    }

    enum NiClientProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            boolean valid = data.getBooleanOr(KEY_VALID, false);

            if (!valid) {
                tooltip.add(Component.translatable("jade.tremendousstorage.network_interface.invalid")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            int fePerTick = data.getIntOr(KEY_FE_PER_TICK, 0);
            tooltip.add(Component.translatable("jade.tremendousstorage.network_interface.power_draw", fePerTick));

            boolean powered = data.getBooleanOr(KEY_POWERED, false);
            if (powered) {
                tooltip.add(Component.translatable("jade.tremendousstorage.network_interface.active")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("jade.tremendousstorage.network_interface.inactive")
                        .withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public Identifier getUid() {
            return NI_PROVIDER;
        }
    }
}
