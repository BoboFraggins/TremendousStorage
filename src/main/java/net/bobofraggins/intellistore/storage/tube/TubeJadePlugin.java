package net.bobofraggins.intellistore.storage.tube;

import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storage.tubeattachments.AttachmentType;
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
 * Jade (WAILA) plugin for Tube blocks.
 *
 * <p>When looking at a tube that has an Import or Export Interface attachment,
 * appends "Not Enough Power" if the connected network is unpowered.
 */
@WailaPlugin
public class TubeJadePlugin implements IWailaPlugin {

    static final ResourceLocation TUBE_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "tube_power");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TubeDataProvider.INSTANCE, TubeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TubeDataProvider.INSTANCE, TubeBlock.class);
    }

    enum TubeDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_HAS_ATTACHMENT = "HasAttachment";
        private static final String KEY_POWERED = "Powered";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TubeBlockEntity be)) return;

            // Check if any attachment is installed
            boolean hasAttachment = false;
            for (int i = 0; i < 6; i++) {
                if (be.getAttachmentType(i) != AttachmentType.NONE) {
                    hasAttachment = true;
                    break;
                }
            }
            data.putBoolean(KEY_HAS_ATTACHMENT, hasAttachment);

            if (hasAttachment) {
                NetworkItemHandler network = be.getNetworkView();
                if (network != null) {
                    NetworkInterfaceBlockEntity ni = network.getNetworkInterface();
                    data.putBoolean(KEY_POWERED, ni == null || ni.isPowered());
                } else {
                    data.putBoolean(KEY_POWERED, false);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return TUBE_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.getBoolean(KEY_HAS_ATTACHMENT) && !data.getBoolean(KEY_POWERED)) {
                tooltip.add(Component.translatable("jade.intellistore.not_enough_power")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }
}
