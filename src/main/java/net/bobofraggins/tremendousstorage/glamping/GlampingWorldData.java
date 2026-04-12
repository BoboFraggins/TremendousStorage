package net.bobofraggins.tremendousstorage.glamping;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-side saved data that tracks two things:
 * <ol>
 *   <li>The sequential camp-allocation counter used to give each new portal its own space.</li>
 *   <li>Per-player return targets — where each player was when they last entered the dimension,
 *       so the {@link TentDoorBlock} can send them back.</li>
 * </ol>
 */
public class GlampingWorldData extends SavedData {

    private static final String DATA_NAME = TremendousStorage.MODID + "_glamping";

    // -------------------------------------------------------------------------
    // ReturnTarget
    // -------------------------------------------------------------------------

    /**
     * Snapshot of a player's location before they entered the Glamping Dimension.
     */
    public record ReturnTarget(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {}

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private int nextCampIndex = 0;
    private final Map<UUID, ReturnTarget> returnTargets = new HashMap<>();

    // -------------------------------------------------------------------------
    // Factory / loading
    // -------------------------------------------------------------------------

    private static final Factory<GlampingWorldData> FACTORY =
            new Factory<>(GlampingWorldData::new, GlampingWorldData::load, null);

    public static GlampingWorldData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    // -------------------------------------------------------------------------
    // Camp allocation
    // -------------------------------------------------------------------------

    /**
     * Allocates and returns the origin (bottom-north-west corner) of the next camp.
     * Y is always {@link GlampingDimension#CAMP_BOTTOM_Y}.
     */
    public BlockPos allocateNewCamp() {
        int x = nextCampIndex * GlampingDimension.CAMP_SPACING;
        BlockPos origin = new BlockPos(x, GlampingDimension.CAMP_BOTTOM_Y, 0);
        nextCampIndex++;
        setDirty();
        return origin;
    }

    // -------------------------------------------------------------------------
    // Return targets
    // -------------------------------------------------------------------------

    public void storeReturnTarget(
            UUID playerId, ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
        returnTargets.put(playerId, new ReturnTarget(dimension, x, y, z, yRot, xRot));
        setDirty();
    }

    @Nullable
    public ReturnTarget getReturnTarget(UUID playerId) {
        return returnTargets.get(playerId);
    }

    // -------------------------------------------------------------------------
    // Serialisation
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("nextCampIndex", nextCampIndex);

        ListTag list = new ListTag();
        for (Map.Entry<UUID, ReturnTarget> entry : returnTargets.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", entry.getKey());
            ReturnTarget r = entry.getValue();
            t.putString("dim", r.dimension().location().toString());
            t.putDouble("x", r.x());
            t.putDouble("y", r.y());
            t.putDouble("z", r.z());
            t.putFloat("yRot", r.yRot());
            t.putFloat("xRot", r.xRot());
            list.add(t);
        }
        tag.put("returnTargets", list);
        return tag;
    }

    public static GlampingWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        GlampingWorldData data = new GlampingWorldData();
        data.nextCampIndex = tag.getInt("nextCampIndex");

        ListTag list = tag.getList("returnTargets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            UUID uuid = t.getUUID("uuid");
            ResourceKey<Level> dim =
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim")));
            data.returnTargets.put(
                    uuid,
                    new ReturnTarget(
                            dim,
                            t.getDouble("x"),
                            t.getDouble("y"),
                            t.getDouble("z"),
                            t.getFloat("yRot"),
                            t.getFloat("xRot")));
        }
        return data;
    }
}
