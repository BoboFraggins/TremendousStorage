package net.bobofraggins.tremendousstorage.storage.endertank;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Server-side persistent storage for Ender Tremendous Tank shared fluid inventories.
 *
 * <p>Each linked pair of Ender Tanks shares a 64-bit {@code linkId}. This map stores the
 * authoritative {@link FluidStack} type (amount=1) and fill level for each link ID.
 */
public class EnderTankStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_tanks";

    private static final Codec<EnderTankStorage> CODEC =
            CompoundTag.CODEC.xmap(EnderTankStorage::fromCompoundTag, EnderTankStorage::toCompoundTag);

    static final SavedDataType<EnderTankStorage> TYPE =
            new SavedDataType<>(net.minecraft.resources.Identifier.parse(SAVE_KEY), EnderTankStorage::new, CODEC);

    private record FluidState(FluidStack type, long amount) {}

    private final Map<Long, FluidState> tanks = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();
    private final Map<Long, StorageTier> tiers = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderTankStorage get(MinecraftServer server) {
        SavedDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    // -------------------------------------------------------------------------
    // Read / write
    // -------------------------------------------------------------------------

    public boolean hasLink(long linkId) {
        return tanks.containsKey(linkId);
    }

    /** Returns a monotonically increasing counter that increments on every {@link #setState} call. */
    public long getVersion(long linkId) {
        return versions.getOrDefault(linkId, 0L);
    }

    /** Returns the stored fluid type (amount=1), or EMPTY if unset. */
    public FluidStack getStoredFluid(long linkId) {
        FluidState state = tanks.get(linkId);
        return state != null ? state.type() : FluidStack.EMPTY;
    }

    /** Returns the stored fill amount in mB, or 0 if unset. */
    public long getAmount(long linkId) {
        FluidState state = tanks.get(linkId);
        return state != null ? state.amount() : 0L;
    }

    public StorageTier getTier(long linkId) {
        return tiers.getOrDefault(linkId, StorageTier.WOOD);
    }

    public void setTier(long linkId, StorageTier tier) {
        tiers.put(linkId, tier);
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    /** Writes the fluid state for the given link ID and marks dirty. */
    public void setState(long linkId, FluidStack type, long amount) {
        tanks.put(linkId, new FluidState(type.isEmpty() ? FluidStack.EMPTY : type.copyWithAmount(1), amount));
        versions.merge(linkId, 1L, Long::sum);
        setDirty();
    }

    /**
     * Initialises a new link entry. Does nothing if the link ID is already registered
     * (so the second tank placed doesn't overwrite the shared contents with its own stale state).
     */
    public void initLink(long linkId, FluidStack type, long amount, StorageTier tier) {
        if (!tanks.containsKey(linkId)) {
            tanks.put(linkId, new FluidState(type.isEmpty() ? FluidStack.EMPTY : type.copyWithAmount(1), amount));
            tiers.put(linkId, tier);
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence via Codec
    // -------------------------------------------------------------------------

    private static EnderTankStorage fromCompoundTag(CompoundTag tag) {
        EnderTankStorage storage = new EnderTankStorage();
        ListTag links = tag.getListOrEmpty("Links");
        for (int i = 0; i < links.size(); i++) {
            CompoundTag entry = links.getCompoundOrEmpty(i);
            long linkId = entry.getLongOr("LinkId", 0L);
            if (linkId == 0L) continue;
            FluidStack type = entry.getCompound("Fluid")
                    .flatMap(ft ->
                            FluidStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, ft).result())
                    .orElse(FluidStack.EMPTY);
            long amount = entry.getLongOr("Amount", 0L);
            storage.tanks.put(linkId, new FluidState(type, amount));
            entry.getString("Tier").ifPresent(tier -> storage.tiers.put(linkId, StorageTier.fromId(tier)));
        }
        return storage;
    }

    private CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        ListTag links = new ListTag();
        for (Map.Entry<Long, FluidState> entry : tanks.entrySet()) {
            long linkId = entry.getKey();
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", linkId);
            FluidStack type = entry.getValue().type();
            if (!type.isEmpty()) {
                FluidStack.OPTIONAL_CODEC
                        .encodeStart(NbtOps.INSTANCE, type)
                        .result()
                        .ifPresent(nbt -> e.put("Fluid", nbt));
            }
            e.putLong("Amount", entry.getValue().amount());
            StorageTier tier = tiers.getOrDefault(linkId, StorageTier.WOOD);
            if (tier != StorageTier.WOOD) e.putString("Tier", tier.getId());
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
