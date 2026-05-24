package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.power.stirlingengine.StirlingEngineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends Stirling Engine generation rate and stored energy to the Jade client. */
public enum StirlingEngineJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final Identifier UID = Identifier.fromNamespaceAndPath("tremendousstorage", "stirling_engine_power");

    static final String KEY_GENERATION = "Generation";
    static final String KEY_STORED = "EnergyStored";
    static final String KEY_MAX = "MaxEnergy";

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof StirlingEngineBlockEntity engine)) return;
        data.putInt(KEY_GENERATION, engine.getHeatGeneration());
        data.putInt(KEY_STORED, engine.getEnergyStored());
        data.putInt(KEY_MAX, engine.getMaxEnergy());
    }
}
