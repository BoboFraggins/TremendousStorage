package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.glamping.present.PresentBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum PresentJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final Identifier UID = Identifier.fromNamespaceAndPath("tremendousstorage", "present");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof PresentBlockEntity present)) return;
        BlockState wrapped = present.getWrappedState();
        if (wrapped == null) return;
        Identifier id = BuiltInRegistries.BLOCK.getKey(wrapped.getBlock());
        if (id != null) data.putString("WrappedBlock", id.toString());
    }
}
