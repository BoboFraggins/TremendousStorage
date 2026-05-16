package net.bobofraggins.tremendousstorage.storage.enderchest;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Server-side persistent storage for Ender Tremendous Chest shared inventories.
 *
 * <p>Each linked pair of Ender Chests shares a 64-bit {@code linkId}. This map stores the
 * authoritative serialised item list (a "Types" {@link ListTag}) for each link ID.
 */
public class EnderChestStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_chests";

    private static final Codec<EnderChestStorage> CODEC =
            CompoundTag.CODEC.xmap(EnderChestStorage::fromCompoundTag, EnderChestStorage::toCompoundTag);

    static final SavedDataType<EnderChestStorage> TYPE =
            new SavedDataType<>(net.minecraft.resources.Identifier.parse(SAVE_KEY), EnderChestStorage::new, CODEC);

    private final Map<Long, ListTag> inventories = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();
    private final Map<Long, StorageTier> tiers = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderChestStorage get(MinecraftServer server) {
        SavedDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    // -------------------------------------------------------------------------
    // Read / write
    // -------------------------------------------------------------------------

    public boolean hasLink(long linkId) {
        return inventories.containsKey(linkId);
    }

    /** Returns a monotonically increasing counter that increments on every {@link #setTypes} call. */
    public long getVersion(long linkId) {
        return versions.getOrDefault(linkId, 0L);
    }

    /**
     * Returns the inventory tag for the given link ID, or an empty list tag if none exists.
     */
    public ListTag getTypes(long linkId) {
        return inventories.getOrDefault(linkId, new ListTag());
    }

    public StorageTier getTier(long linkId) {
        return tiers.getOrDefault(linkId, StorageTier.WOOD);
    }

    /**
     * Stores the tier for the given link ID, bumps the version counter, and marks dirty.
     */
    public void setTier(long linkId, StorageTier tier) {
        tiers.put(linkId, tier);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    /**
     * Stores the inventory tag for the given link ID and marks this data as dirty.
     */
    public void setTypes(long linkId, ListTag types) {
        inventories.put(linkId, types);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    /**
     * Initialises a new link entry with the given inventory. Does nothing if the link ID is
     * already registered.
     */
    public void initLink(long linkId, ListTag types, StorageTier tier) {
        if (!inventories.containsKey(linkId)) {
            inventories.put(linkId, types);
            tiers.put(linkId, tier);
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence via Codec
    // -------------------------------------------------------------------------

    private static EnderChestStorage fromCompoundTag(CompoundTag tag) {
        EnderChestStorage storage = new EnderChestStorage();
        ListTag links = tag.getListOrEmpty("Links");
        for (int i = 0; i < links.size(); i++) {
            CompoundTag entry = links.getCompoundOrEmpty(i);
            long linkId = entry.getLongOr("LinkId", 0L);
            if (linkId == 0L) continue;
            storage.inventories.put(linkId, entry.getListOrEmpty("Types"));
            entry.getString("Tier").ifPresent(tier -> storage.tiers.put(linkId, StorageTier.fromId(tier)));
        }
        return storage;
    }

    private CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        ListTag links = new ListTag();
        for (Map.Entry<Long, ListTag> entry : inventories.entrySet()) {
            long linkId = entry.getKey();
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", linkId);
            e.put("Types", entry.getValue());
            StorageTier tier = tiers.getOrDefault(linkId, StorageTier.WOOD);
            if (tier != StorageTier.WOOD) e.putString("Tier", tier.getId());
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
