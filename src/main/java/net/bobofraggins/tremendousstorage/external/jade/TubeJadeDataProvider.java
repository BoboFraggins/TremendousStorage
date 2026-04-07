package net.bobofraggins.tremendousstorage.external.jade;

import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Syncs the six attachment types from the server-side {@link TubeBlockEntity} to the client
 * so {@link TubeJadeComponentProvider} can display the correct name.
 */
public enum TubeJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("tremendousstorage", "tube");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof TubeBlockEntity be)) return;
        byte[] types = new byte[6];
        for (int i = 0; i < 6; i++) {
            types[i] = (byte) be.getAttachmentType(i).ordinal();
        }
        data.putByteArray("AttachmentTypes", types);
    }
}
