package net.bobofraggins.intellistore.storage.tube;

import net.bobofraggins.intellistore.storage.tubeattachments.AttachmentType;
import net.minecraft.nbt.CompoundTag;
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
 * <p>Shows tube attachment information in the Jade HUD.
 */
@WailaPlugin
public class TubeJadePlugin implements IWailaPlugin {

    static final ResourceLocation TUBE_PROVIDER = ResourceLocation.fromNamespaceAndPath("intellistore", "tube_info");

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

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TubeBlockEntity be)) return;

            boolean hasAttachment = false;
            for (int i = 0; i < 6; i++) {
                if (be.getAttachmentType(i) != AttachmentType.NONE) {
                    hasAttachment = true;
                    break;
                }
            }
            data.putBoolean(KEY_HAS_ATTACHMENT, hasAttachment);
        }

        @Override
        public ResourceLocation getUid() {
            return TUBE_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            // No additional tooltip content needed currently
        }
    }
}
