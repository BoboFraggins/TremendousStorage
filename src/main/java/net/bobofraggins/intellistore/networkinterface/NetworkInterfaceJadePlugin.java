package net.bobofraggins.intellistore.networkinterface;

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
 * Jade (WAILA) plugin for the Network Interface.
 *
 * <p>Appends:
 * <ul>
 *   <li>Total FE/t consumed by the network
 *   <li>"Not Enough Power" when the network is unpowered
 * </ul>
 */
@WailaPlugin
public class NetworkInterfaceJadePlugin implements IWailaPlugin {

    static final ResourceLocation NI_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "network_interface_power");

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

        private static final String KEY_POWERED = "Powered";
        private static final String KEY_CONSUMPTION = "Consumption";
        private static final String KEY_STORED = "EnergyStored";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof NetworkInterfaceBlockEntity be)) return;
            data.putBoolean(KEY_POWERED, be.isPowered());
            data.putInt(KEY_CONSUMPTION, be.getTotalConsumption());
            data.putInt(KEY_STORED, be.getEnergyStored());
        }

        @Override
        public ResourceLocation getUid() {
            return NI_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            int consumption = data.getInt(KEY_CONSUMPTION);
            boolean powered = data.getBoolean(KEY_POWERED);

            // Always show total consumption
            tooltip.add(Component.translatable("jade.intellistore.network_interface.consumption",
                    CountFormat.format(consumption)));

            if (!powered) {
                tooltip.add(Component.translatable("jade.intellistore.not_enough_power")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }
}
