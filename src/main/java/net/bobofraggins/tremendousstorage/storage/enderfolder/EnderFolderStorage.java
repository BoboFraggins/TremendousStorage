package net.bobofraggins.tremendousstorage.storage.enderfolder;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
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

    private static final String SAVE_KEY = "tremendousstorage_ender_folders";

    private static final Codec<EnderFolderStorage> CODEC =
            CompoundTag.CODEC.xmap(EnderFolderStorage::fromCompoundTag, EnderFolderStorage::toCompoundTag);

    static final SavedDataType<EnderFolderStorage> TYPE =
            new SavedDataType<>(SAVE_KEY, ctx -> new EnderFolderStorage(), ctx -> CODEC);

    private final Map<Long, FolderContents> contents = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderFolderStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
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
    // NBT persistence via Codec
    // -------------------------------------------------------------------------

    private static EnderFolderStorage fromCompoundTag(CompoundTag tag) {
        EnderFolderStorage storage = new EnderFolderStorage();
        ListTag list = tag.getListOrEmpty("Links");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            long linkId = entry.getLongOr("LinkId", 0L);
            if (linkId == 0L) continue;
            FolderContents fc = entry.getCompound("Contents")
                    .flatMap(ct ->
                            FolderContents.CODEC.parse(NbtOps.INSTANCE, ct).result())
                    .orElse(FolderContents.EMPTY);
            storage.contents.put(linkId, fc);
        }
        return storage;
    }

    private CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<Long, FolderContents> entry : contents.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", entry.getKey());
            FolderContents.CODEC
                    .encodeStart(NbtOps.INSTANCE, entry.getValue())
                    .result()
                    .ifPresent(nbt -> e.put("Contents", nbt));
            list.add(e);
        }
        tag.put("Links", list);
        return tag;
    }
}
