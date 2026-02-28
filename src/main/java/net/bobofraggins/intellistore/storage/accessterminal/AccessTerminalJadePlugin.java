package net.bobofraggins.intellistore.storage.accessterminal;

import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
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
 * <p>Appends "Not Enough Power" when the connected network is unpowered.
 */
@WailaPlugin
public class AccessTerminalJadePlugin implements IWailaPlugin {

    static final ResourceLocation SAT_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "sat_power");

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

        private static final String KEY_POWERED = "Powered";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel serverLevel)) return;
            net.minecraft.core.BlockPos niPos =
                    AccessTerminalBFS.findNI(serverLevel, accessor.getPosition());
            if (niPos == null) {
                data.putBoolean(KEY_POWERED, false);
                return;
            }
            if (serverLevel.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
                data.putBoolean(KEY_POWERED, ni.isPowered());
            } else {
                data.putBoolean(KEY_POWERED, false);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return SAT_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains(KEY_POWERED) && !data.getBoolean(KEY_POWERED)) {
                tooltip.add(Component.translatable("jade.intellistore.not_enough_power")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }
}
