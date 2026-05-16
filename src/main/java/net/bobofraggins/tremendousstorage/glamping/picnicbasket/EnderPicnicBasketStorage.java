package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Server-side persistent storage for Ender Picnic Basket shared inventories.
 *
 * <p>Each linked pair of Ender Picnic Baskets shares a 64-bit {@code linkId}. This map stores
 * the authoritative serialised item list for each link ID.
 */
public class EnderPicnicBasketStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_picnic_baskets";

    private static final Codec<EnderPicnicBasketStorage> CODEC =
            CompoundTag.CODEC.xmap(EnderPicnicBasketStorage::fromCompoundTag, EnderPicnicBasketStorage::toCompoundTag);

    static final SavedDataType<EnderPicnicBasketStorage> TYPE = new SavedDataType<>(
            net.minecraft.resources.Identifier.parse(SAVE_KEY), EnderPicnicBasketStorage::new, CODEC);

    private final Map<Long, ListTag> inventories = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderPicnicBasketStorage get(MinecraftServer server) {
        SavedDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
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

    public void initLink(long linkId, ListTag types) {
        if (!inventories.containsKey(linkId)) {
            inventories.put(linkId, types);
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence via Codec
    // -------------------------------------------------------------------------

    private static EnderPicnicBasketStorage fromCompoundTag(CompoundTag tag) {
        EnderPicnicBasketStorage storage = new EnderPicnicBasketStorage();
        ListTag links = tag.getListOrEmpty("Links");
        for (int i = 0; i < links.size(); i++) {
            CompoundTag entry = links.getCompoundOrEmpty(i);
            long linkId = entry.getLongOr("LinkId", 0L);
            if (linkId == 0L) continue;
            storage.inventories.put(linkId, entry.getListOrEmpty("Types"));
        }
        return storage;
    }

    private CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
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
