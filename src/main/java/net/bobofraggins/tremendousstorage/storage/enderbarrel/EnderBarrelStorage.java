package net.bobofraggins.tremendousstorage.storage.enderbarrel;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
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

    private static final Codec<EnderBarrelStorage> CODEC =
            CompoundTag.CODEC.xmap(EnderBarrelStorage::fromCompoundTag, EnderBarrelStorage::toCompoundTag);

    static final SavedDataType<EnderBarrelStorage> TYPE =
            new SavedDataType<>(SAVE_KEY, ctx -> new EnderBarrelStorage(), ctx -> CODEC);

    private final Map<Long, ItemStack> storedItems = new HashMap<>();
    private final Map<Long, Long> counts = new HashMap<>();
    private final Map<Long, StorageTier> tiers = new HashMap<>();
    private final Map<Long, Integer> baseSlots = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderBarrelStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
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

    public int getBaseSlot(long linkId) {
        return baseSlots.getOrDefault(linkId, 0);
    }

    public void setContents(long linkId, ItemStack storedItem, long count) {
        storedItems.put(linkId, storedItem.isEmpty() ? ItemStack.EMPTY : storedItem.copyWithCount(1));
        counts.put(linkId, count);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    public void setContentsAndBaseSlot(long linkId, ItemStack storedItem, long count, int baseSlot) {
        storedItems.put(linkId, storedItem.isEmpty() ? ItemStack.EMPTY : storedItem.copyWithCount(1));
        counts.put(linkId, count);
        baseSlots.put(linkId, baseSlot);
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
    // NBT persistence via Codec
    // -------------------------------------------------------------------------

    private static EnderBarrelStorage fromCompoundTag(CompoundTag tag) {
        EnderBarrelStorage s = new EnderBarrelStorage();
        ListTag links = tag.getListOrEmpty("Links");
        for (int i = 0; i < links.size(); i++) {
            CompoundTag e = links.getCompoundOrEmpty(i);
            long linkId = e.getLongOr("LinkId", 0L);
            if (linkId == 0L) continue;
            ItemStack item = e.getCompound("Item")
                    .flatMap(it ->
                            ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, it).result())
                    .orElse(ItemStack.EMPTY);
            if (!item.isEmpty()) item = item.copyWithCount(1);
            s.storedItems.put(linkId, item);
            s.counts.put(linkId, e.getLongOr("Count", 0L));
            e.getString("Tier").ifPresent(tier -> s.tiers.put(linkId, StorageTier.fromId(tier)));
            int baseSlot = e.getIntOr("BaseSlot", 0);
            if (baseSlot != 0) s.baseSlots.put(linkId, baseSlot);
        }
        return s;
    }

    private CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        ListTag links = new ListTag();
        for (Map.Entry<Long, ItemStack> entry : storedItems.entrySet()) {
            long linkId = entry.getKey();
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", linkId);
            ItemStack item = entry.getValue();
            if (!item.isEmpty()) {
                ItemStack.OPTIONAL_CODEC
                        .encodeStart(NbtOps.INSTANCE, item)
                        .result()
                        .ifPresent(t -> e.put("Item", t));
            }
            e.putLong("Count", counts.getOrDefault(linkId, 0L));
            StorageTier tier = tiers.getOrDefault(linkId, StorageTier.WOOD);
            if (tier != StorageTier.WOOD) e.putString("Tier", tier.getId());
            int baseSlot = baseSlots.getOrDefault(linkId, 0);
            if (baseSlot != 0) e.putInt("BaseSlot", baseSlot);
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
