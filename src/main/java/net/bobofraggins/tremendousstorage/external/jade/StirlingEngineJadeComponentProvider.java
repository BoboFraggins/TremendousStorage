package net.bobofraggins.tremendousstorage.external.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltip lines for the Stirling Engine:
 *
 * <ul>
 *   <li>When heated: generation rate ("80 FE/t") and stored energy ("12,000 / 100,000 FE")
 *   <li>When not heated: hint to place a heat source below
 * </ul>
 */
public enum StirlingEngineJadeComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return StirlingEngineJadeDataProvider.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        int generation = data.getIntOr(StirlingEngineJadeDataProvider.KEY_GENERATION, 0);
        int stored = data.getIntOr(StirlingEngineJadeDataProvider.KEY_STORED, 0);
        int max = data.getIntOr(StirlingEngineJadeDataProvider.KEY_MAX, 0);

        if (generation > 0) {
            tooltip.add(Component.translatable("jade.tremendousstorage.stirling_engine.generating", generation));
            tooltip.add(Component.translatable(
                    "jade.tremendousstorage.stirling_engine.stored",
                    String.format("%,d", stored),
                    String.format("%,d", max)));
        } else {
            tooltip.add(Component.translatable("jade.tremendousstorage.stirling_engine.hint"));
        }
    }
}
