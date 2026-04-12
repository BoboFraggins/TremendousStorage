package net.bobofraggins.tremendousstorage.glamping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-side saved data tracking:
 * <ol>
 *   <li>Sequential portal camp allocation.</li>
 *   <li>The full set of claimed camp origins (portal and tent alike), used to
 *       detect collisions when a tent derives its camp from a name seed.</li>
 *   <li>Per-player return targets so the {@link TentDoorBlock} knows where to
 *       send each player home.</li>
 * </ol>
 */
public class GlampingWorldData extends SavedData {

    private static final String DATA_NAME = TremendousStorage.MODID + "_glamping";

    // -------------------------------------------------------------------------
    // ReturnTarget
    // -------------------------------------------------------------------------

    public record ReturnTarget(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {}

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private int nextCampIndex = 0;
    private final Set<Long> claimedCamps = new HashSet<>();
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
    // Portal camp allocation (sequential)
    // -------------------------------------------------------------------------

    /**
     * Allocates and returns the origin of the next portal camp.
     * Also marks it as claimed so tent seed derivation cannot collide with it.
     */
    public BlockPos allocateNewCamp() {
        int x = nextCampIndex * GlampingDimension.CAMP_SPACING;
        BlockPos origin = new BlockPos(x, GlampingDimension.CAMP_BOTTOM_Y, 0);
        nextCampIndex++;
        claimedCamps.add(origin.asLong());
        setDirty();
        return origin;
    }

    // -------------------------------------------------------------------------
    // Tent camp claiming (seed-derived)
    // -------------------------------------------------------------------------

    public boolean isClaimed(BlockPos origin) {
        return claimedCamps.contains(origin.asLong());
    }

    public void claimCamp(BlockPos origin) {
        claimedCamps.add(origin.asLong());
        setDirty();
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

        long[] claimed = claimedCamps.stream().mapToLong(Long::longValue).toArray();
        tag.put("claimedCamps", new LongArrayTag(claimed));

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

        if (tag.contains("claimedCamps", Tag.TAG_LONG_ARRAY)) {
            for (long v : tag.getLongArray("claimedCamps")) {
                data.claimedCamps.add(v);
            }
        }

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
