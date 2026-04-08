package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Server-side persistent storage for Ender Tremendous Backpack shared inventories.
 *
 * <p>Mirrors {@link EnderChestStorage} but keyed separately so backpack link IDs never
 * collide with chest link IDs (the crafting system keeps them distinct anyway, but the
 * separation is an extra safety net).
 */
public class EnderBackpackStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_backpacks";

    private final Map<Long, ListTag> inventories = new HashMap<>();

    public static EnderBackpackStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(EnderBackpackStorage::new, EnderBackpackStorage::load, null), SAVE_KEY);
    }

    public boolean hasLink(long linkId) {
        return inventories.containsKey(linkId);
    }

    public ListTag getTypes(long linkId) {
        return inventories.getOrDefault(linkId, new ListTag());
    }

    public void setTypes(long linkId, ListTag types) {
        inventories.put(linkId, types);
        setDirty();
    }

    public void initLink(long linkId, ListTag types) {
        if (!inventories.containsKey(linkId)) {
            inventories.put(linkId, types);
            setDirty();
        }
    }

    private static EnderBackpackStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderBackpackStorage storage = new EnderBackpackStorage();
        ListTag links = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < links.size(); i++) {
            CompoundTag entry = links.getCompound(i);
            long linkId = entry.getLong("LinkId");
            ListTag types = entry.getList("Types", Tag.TAG_COMPOUND);
            storage.inventories.put(linkId, types);
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag links = new ListTag();
        for (Map.Entry<Long, ListTag> entry : inventories.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", entry.getKey());
            e.put("Types", entry.getValue());
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
