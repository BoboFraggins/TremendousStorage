package net.bobofraggins.tremendousstorage.glamping;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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

    private static final Codec<GlampingWorldData> CODEC =
            CompoundTag.CODEC.xmap(GlampingWorldData::fromCompoundTag, GlampingWorldData::toCompoundTag);

    static final SavedDataType<GlampingWorldData> TYPE =
            new SavedDataType<>(net.minecraft.resources.Identifier.parse(DATA_NAME), GlampingWorldData::new, CODEC);

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
    // Static access
    // -------------------------------------------------------------------------

    public static GlampingWorldData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // -------------------------------------------------------------------------
    // Portal camp allocation (sequential)
    // -------------------------------------------------------------------------

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
    // Serialisation via Codec
    // -------------------------------------------------------------------------

    private static GlampingWorldData fromCompoundTag(CompoundTag tag) {
        GlampingWorldData data = new GlampingWorldData();
        data.nextCampIndex = tag.getIntOr("nextCampIndex", 0);

        for (long v : tag.getLongArray("claimedCamps").orElse(new long[0])) {
            data.claimedCamps.add(v);
        }

        ListTag list = tag.getListOrEmpty("returnTargets");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompoundOrEmpty(i);
            String uuidStr = t.getStringOr("uuid", "");
            if (uuidStr.isEmpty()) continue;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ResourceKey<Level> dim = ResourceKey.create(
                        Registries.DIMENSION, Identifier.parse(t.getStringOr("dim", "minecraft:overworld")));
                data.returnTargets.put(
                        uuid,
                        new ReturnTarget(
                                dim,
                                t.getDoubleOr("x", 0.0),
                                t.getDoubleOr("y", 64.0),
                                t.getDoubleOr("z", 0.0),
                                t.getFloatOr("yRot", 0f),
                                t.getFloatOr("xRot", 0f)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    private CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("nextCampIndex", nextCampIndex);

        long[] claimed = claimedCamps.stream().mapToLong(Long::longValue).toArray();
        tag.put("claimedCamps", new LongArrayTag(claimed));

        ListTag list = new ListTag();
        for (Map.Entry<UUID, ReturnTarget> entry : returnTargets.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("uuid", entry.getKey().toString());
            ReturnTarget r = entry.getValue();
            t.putString("dim", r.dimension().identifier().toString());
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
}
