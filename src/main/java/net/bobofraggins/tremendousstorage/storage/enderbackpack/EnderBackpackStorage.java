package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
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
    private final Map<Long, Long> versions = new HashMap<>();
    private final Map<Long, StorageTier> tiers = new HashMap<>();
    private final Map<Long, Boolean> craftingUpgrades = new HashMap<>();

    public static EnderBackpackStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(EnderBackpackStorage::new, EnderBackpackStorage::load, null), SAVE_KEY);
    }

    public boolean hasLink(long linkId) {
        return inventories.containsKey(linkId);
    }

    /** Returns a monotonically increasing counter that increments on every {@link #setTypes} call. */
    public long getVersion(long linkId) {
        return versions.getOrDefault(linkId, 0L);
    }

    public ListTag getTypes(long linkId) {
        return inventories.getOrDefault(linkId, new ListTag());
    }

    public StorageTier getTier(long linkId) {
        return tiers.getOrDefault(linkId, StorageTier.WOOD);
    }

    public boolean hasCraftingUpgrade(long linkId) {
        return craftingUpgrades.getOrDefault(linkId, false);
    }

    /** Marks the link as having a crafting upgrade, bumps the version, and marks dirty. */
    public void setCraftingUpgrade(long linkId) {
        craftingUpgrades.put(linkId, true);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    public void setTier(long linkId, StorageTier tier) {
        tiers.put(linkId, tier);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    public void setTypes(long linkId, ListTag types) {
        inventories.put(linkId, types);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    public void initLink(long linkId, ListTag types, StorageTier tier, boolean craftingUpgrade) {
        if (!inventories.containsKey(linkId)) {
            inventories.put(linkId, types);
            tiers.put(linkId, tier);
            if (craftingUpgrade) craftingUpgrades.put(linkId, true);
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
            if (entry.contains("Tier")) {
                storage.tiers.put(linkId, StorageTier.fromId(entry.getString("Tier")));
            }
            if (entry.getBoolean("CraftingUpgrade")) {
                storage.craftingUpgrades.put(linkId, true);
            }
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag links = new ListTag();
        for (Map.Entry<Long, ListTag> entry : inventories.entrySet()) {
            long linkId = entry.getKey();
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", linkId);
            e.put("Types", entry.getValue());
            StorageTier tier = tiers.getOrDefault(linkId, StorageTier.WOOD);
            if (tier != StorageTier.WOOD) {
                e.putString("Tier", tier.getId());
            }
            if (craftingUpgrades.getOrDefault(linkId, false)) {
                e.putBoolean("CraftingUpgrade", true);
            }
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
