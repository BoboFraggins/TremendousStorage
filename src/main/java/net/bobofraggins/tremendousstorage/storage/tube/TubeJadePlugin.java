package net.bobofraggins.tremendousstorage.storage.tube;

import net.bobofraggins.tremendousstorage.storage.tubeattachments.AttachmentType;
import net.minecraft.nbt.CompoundTag;
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
 * Jade (WAILA) plugin for Tube blocks.
 *
 * <p>Shows tube attachment information in the Jade HUD.
 */
@WailaPlugin
public class TubeJadePlugin implements IWailaPlugin {

    static final Identifier TUBE_PROVIDER = Identifier.fromNamespaceAndPath("tremendousstorage", "tube_info");

    private static final String KEY_HAS_ATTACHMENT = "HasAttachment";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TubeServerProvider.INSTANCE, TubeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TubeClientProvider.INSTANCE, TubeBlock.class);
    }

    enum TubeServerProvider implements snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

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
        public Identifier getUid() {
            return TUBE_PROVIDER;
        }
    }

    enum TubeClientProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            // No additional tooltip content needed currently
        }

        @Override
        public Identifier getUid() {
            return TUBE_PROVIDER;
        }
    }
}
