package net.bobofraggins.tremendousstorage.storage.enderbarrel;

import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Server-side persistent storage for Ender Barrel shared contents.
 *
 * <p>Each linked pair of Ender Barrels shares a 64-bit {@code linkId}. This maps each
 * link ID to the authoritative stored item type, count, and tier. When either barrel's
 * contents change the new state is written here so both barrels stay in sync.
 */
public class EnderBarrelStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_barrels";

    private final Map<Long, ItemStack> storedItems = new HashMap<>();
    private final Map<Long, Long> counts = new HashMap<>();
    private final Map<Long, StorageTier> tiers = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderBarrelStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(EnderBarrelStorage::new, EnderBarrelStorage::load, null), SAVE_KEY);
    }

    // -------------------------------------------------------------------------
    // Read / write
    // -------------------------------------------------------------------------

    public boolean hasLink(long linkId) {
        return storedItems.containsKey(linkId);
    }

    public long getVersion(long linkId) {
        return versions.getOrDefault(linkId, 0L);
    }

    /** Returns the stored item type key (count=1), or EMPTY if unlocked. */
    public ItemStack getStoredItem(long linkId) {
        return storedItems.getOrDefault(linkId, ItemStack.EMPTY);
    }

    public long getCount(long linkId) {
        return counts.getOrDefault(linkId, 0L);
    }

    public StorageTier getTier(long linkId) {
        return tiers.getOrDefault(linkId, StorageTier.WOOD);
    }

    public void setContents(long linkId, ItemStack storedItem, long count) {
        storedItems.put(linkId, storedItem.isEmpty() ? ItemStack.EMPTY : storedItem.copyWithCount(1));
        counts.put(linkId, count);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    public void setTier(long linkId, StorageTier tier) {
        tiers.put(linkId, tier);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    /**
     * Seeds the storage with the given state. Does nothing if the link ID is already registered
     * (i.e. the second barrel is being placed after the first was already used).
     */
    public void initLink(long linkId, ItemStack storedItem, long count, StorageTier tier) {
        if (!storedItems.containsKey(linkId)) {
            storedItems.put(linkId, storedItem.isEmpty() ? ItemStack.EMPTY : storedItem.copyWithCount(1));
            counts.put(linkId, count);
            tiers.put(linkId, tier);
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    private static EnderBarrelStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderBarrelStorage s = new EnderBarrelStorage();
        ListTag links = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < links.size(); i++) {
            CompoundTag e = links.getCompound(i);
            long linkId = e.getLong("LinkId");
            ItemStack item =
                    e.contains("Item") ? ItemStack.parseOptional(registries, e.getCompound("Item")) : ItemStack.EMPTY;
            if (!item.isEmpty()) item = item.copyWithCount(1);
            s.storedItems.put(linkId, item);
            s.counts.put(linkId, e.getLong("Count"));
            if (e.contains("Tier")) {
                s.tiers.put(linkId, StorageTier.fromId(e.getString("Tier")));
            }
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag links = new ListTag();
        for (Map.Entry<Long, ItemStack> entry : storedItems.entrySet()) {
            long linkId = entry.getKey();
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", linkId);
            ItemStack item = entry.getValue();
            if (!item.isEmpty()) e.put("Item", item.save(registries));
            e.putLong("Count", counts.getOrDefault(linkId, 0L));
            StorageTier tier = tiers.getOrDefault(linkId, StorageTier.WOOD);
            if (tier != StorageTier.WOOD) e.putString("Tier", tier.getId());
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
