package net.bobofraggins.tremendousstorage.storage.accessterminal;

import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade (WAILA) plugin for the Storage Access Terminal.
 *
 * <p>Appends "Network Invalid" when the connected network is not valid.
 */
@WailaPlugin
public class AccessTerminalJadePlugin implements IWailaPlugin {

    static final ResourceLocation SAT_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "sat_network_validity");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SatDataProvider.INSTANCE, AccessTerminalBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SatDataProvider.INSTANCE, AccessTerminalBlock.class);
    }

    enum SatDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_VALID = "NetworkValid";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel serverLevel)) return;
            net.minecraft.core.BlockPos niPos = AccessTerminalBFS.findNI(serverLevel, accessor.getPosition());
            if (niPos == null) {
                data.putBoolean(KEY_VALID, false);
                return;
            }
            if (serverLevel.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
                data.putBoolean(KEY_VALID, ni.isNetworkValid());
            } else {
                data.putBoolean(KEY_VALID, false);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return SAT_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains(KEY_VALID) && !data.getBoolean(KEY_VALID)) {
                tooltip.add(Component.translatable("jade.tremendousstorage.network_interface.invalid")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }
}
