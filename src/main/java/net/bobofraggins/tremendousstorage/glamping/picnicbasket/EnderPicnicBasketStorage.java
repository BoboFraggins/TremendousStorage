package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

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
 * Server-side persistent storage for Ender Picnic Basket shared inventories.
 *
 * <p>Each linked pair of Ender Picnic Baskets shares a 64-bit {@code linkId}. This map stores
 * the authoritative serialised item list for each link ID. Simpler than {@link
 * net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestStorage} because the picnic
 * basket has no tier or crafting-upgrade state to track.
 */
public class EnderPicnicBasketStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_picnic_baskets";

    private final Map<Long, ListTag> inventories = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderPicnicBasketStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(EnderPicnicBasketStorage::new, EnderPicnicBasketStorage::load, null), SAVE_KEY);
    }

    // -------------------------------------------------------------------------
    // Read / write
    // -------------------------------------------------------------------------

    public boolean hasLink(long linkId) {
        return inventories.containsKey(linkId);
    }

    public long getVersion(long linkId) {
        return versions.getOrDefault(linkId, 0L);
    }

    public ListTag getTypes(long linkId) {
        return inventories.getOrDefault(linkId, new ListTag());
    }

    public void setTypes(long linkId, ListTag types) {
        inventories.put(linkId, types);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    /**
     * Seeds a new link entry with the given inventory. Does nothing if this link ID is already
     * registered (i.e. the second basket is being placed after the first was already used).
     */
    public void initLink(long linkId, ListTag types) {
        if (!inventories.containsKey(linkId)) {
            inventories.put(linkId, types);
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    private static EnderPicnicBasketStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderPicnicBasketStorage storage = new EnderPicnicBasketStorage();
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
