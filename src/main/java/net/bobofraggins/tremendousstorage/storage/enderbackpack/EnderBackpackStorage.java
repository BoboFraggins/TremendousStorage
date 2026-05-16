package net.bobofraggins.tremendousstorage.storage.enderbackpack;

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
 * Server-side persistent storage for Ender Tremendous Backpack shared inventories.
 *
 * <p>Mirrors {@link EnderChestStorage} but keyed separately so backpack link IDs never
 * collide with chest link IDs (the crafting system keeps them distinct anyway, but the
 * separation is an extra safety net).
 */
public class EnderBackpackStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_backpacks";

    private static final Codec<EnderBackpackStorage> CODEC =
            CompoundTag.CODEC.xmap(EnderBackpackStorage::fromCompoundTag, EnderBackpackStorage::toCompoundTag);

    static final SavedDataType<EnderBackpackStorage> TYPE =
            new SavedDataType<>(net.minecraft.resources.Identifier.parse(SAVE_KEY), EnderBackpackStorage::new, CODEC);

    private final Map<Long, ListTag> inventories = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();
    private final Map<Long, StorageTier> tiers = new HashMap<>();

    public static EnderBackpackStorage get(MinecraftServer server) {
        SavedDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
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

    private static EnderBackpackStorage fromCompoundTag(CompoundTag tag) {
        EnderBackpackStorage storage = new EnderBackpackStorage();
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
