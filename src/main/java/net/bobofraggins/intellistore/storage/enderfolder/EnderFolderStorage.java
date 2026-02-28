package net.bobofraggins.intellistore.storage.enderfolder;

import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Server-side persistent storage for Ender Folder shared inventories.
 *
 * <p>Each linked pair of Ender Folders shares a 64-bit {@code linkId}. This map stores the
 * authoritative {@link FolderContents} for each link ID. When any Ender Folder's contents
 * change, the new state is written here and the item stack is updated. All other items with
 * the same link ID will be synced the next time they are accessed.
 */
public class EnderFolderStorage extends SavedData {

    private static final String SAVE_KEY = "intellistore_ender_folders";

    private final Map<Long, FolderContents> contents = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderFolderStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(EnderFolderStorage::new, EnderFolderStorage::load, null), SAVE_KEY);
    }

    // -------------------------------------------------------------------------
    // Read / write
    // -------------------------------------------------------------------------

    /**
     * Returns the authoritative {@link FolderContents} for the given link ID, or
     * {@link FolderContents#EMPTY} if no entry exists yet.
     */
    public FolderContents getContents(long linkId) {
        return contents.getOrDefault(linkId, FolderContents.EMPTY);
    }

    /**
     * Stores updated {@link FolderContents} for the given link ID and marks this data as dirty.
     */
    public void setContents(long linkId, FolderContents fc) {
        contents.put(linkId, fc);
        setDirty();
    }

    /**
     * Initialises a new link entry (first-time creation during recipe output).
     * Does nothing if the link ID is already registered.
     */
    public void initLink(long linkId) {
        if (!contents.containsKey(linkId)) {
            contents.put(linkId, FolderContents.EMPTY);
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    private static EnderFolderStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderFolderStorage storage = new EnderFolderStorage();
        ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long linkId = entry.getLong("LinkId");
            FolderContents fc = FolderContents.CODEC
                    .parse(net.minecraft.nbt.NbtOps.INSTANCE, entry.get("Contents"))
                    .result()
                    .orElse(FolderContents.EMPTY);
            storage.contents.put(linkId, fc);
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, FolderContents> entry : contents.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", entry.getKey());
            FolderContents.CODEC
                    .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, entry.getValue())
                    .result()
                    .ifPresent(nbt -> e.put("Contents", nbt));
            list.add(e);
        }
        tag.put("Links", list);
        return tag;
    }
}
