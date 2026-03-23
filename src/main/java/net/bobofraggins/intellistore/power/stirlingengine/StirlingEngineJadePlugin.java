package net.bobofraggins.intellistore.power.stirlingengine;

import net.bobofraggins.intellistore.shared.storage.StorageTier;
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
 * Jade (WAILA) plugin for the Stirling Engine.
 *
 * <p>Appends:
 * <ul>
 *   <li>"Place Above Heat Source" hint when not heated
 *   <li>Current FE/t generation rate when heated
 *   <li>Stored energy
 * </ul>
 */
@WailaPlugin
public class StirlingEngineJadePlugin implements IWailaPlugin {

    static final ResourceLocation SE_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("intellistore", "stirling_engine");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SeDataProvider.INSTANCE, StirlingEngineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SeDataProvider.INSTANCE, StirlingEngineBlock.class);
    }

    enum SeDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_HEATED = "Heated";
        private static final String KEY_STORED = "EnergyStored";
        private static final String KEY_MAX = "MaxEnergy";
        private static final String KEY_TIER = "Tier";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof StirlingEngineBlockEntity be)) return;
            data.putBoolean(KEY_HEATED, be.isHeated());
            data.putInt(KEY_STORED, be.getEnergyStored());
            data.putInt(KEY_MAX, be.getMaxEnergy());
            data.putString(KEY_TIER, be.getTier().getId());
        }

        @Override
        public ResourceLocation getUid() {
            return SE_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            boolean heated = data.getBoolean(KEY_HEATED);
            int stored = data.getInt(KEY_STORED);
            int max = data.getInt(KEY_MAX);
            StorageTier tier = StorageTier.fromId(data.getString(KEY_TIER));

            if (tier != StorageTier.PAPER) {
                String tierName = Character.toUpperCase(tier.getId().charAt(0))
                        + tier.getId().substring(1);
                tooltip.add(Component.literal(tierName));
            }
            if (!heated) {
                tooltip.add(Component.translatable("jade.intellistore.stirling_engine.hint"));
            } else {
                tooltip.add(Component.translatable(
                        "jade.intellistore.stirling_engine.stored",
                        net.bobofraggins.intellistore.shared.util.CountFormat.format(stored),
                        net.bobofraggins.intellistore.shared.util.CountFormat.format(max)));
            }
        }
    }
}
