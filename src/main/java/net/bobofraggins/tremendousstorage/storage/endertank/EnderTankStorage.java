package net.bobofraggins.tremendousstorage.storage.endertank;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Server-side persistent storage for Ender Tremendous Tank shared fluid inventories.
 *
 * <p>Each linked pair of Ender Tanks shares a 64-bit {@code linkId}. This map stores the
 * authoritative {@link FluidStack} type (amount=1) and fill level for each link ID.
 */
public class EnderTankStorage extends SavedData {

    private static final String SAVE_KEY = "tremendousstorage_ender_tanks";

    private record FluidState(FluidStack type, long amount) {}

    private final Map<Long, FluidState> tanks = new HashMap<>();
    private final Map<Long, Long> versions = new HashMap<>();

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static EnderTankStorage get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(EnderTankStorage::new, EnderTankStorage::load, null), SAVE_KEY);
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
    public void initLink(long linkId, FluidStack type, long amount) {
        if (!tanks.containsKey(linkId)) {
            tanks.put(linkId, new FluidState(type.isEmpty() ? FluidStack.EMPTY : type.copyWithAmount(1), amount));
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    private static EnderTankStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderTankStorage storage = new EnderTankStorage();
        ListTag links = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < links.size(); i++) {
            CompoundTag entry = links.getCompound(i);
            long linkId = entry.getLong("LinkId");
            FluidStack type = entry.contains("Fluid")
                    ? FluidStack.parseOptional(registries, entry.getCompound("Fluid"))
                    : FluidStack.EMPTY;
            long amount = entry.getLong("Amount");
            storage.tanks.put(linkId, new FluidState(type, amount));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag links = new ListTag();
        for (Map.Entry<Long, FluidState> entry : tanks.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putLong("LinkId", entry.getKey());
            FluidStack type = entry.getValue().type();
            if (!type.isEmpty()) {
                e.put("Fluid", type.save(registries));
            }
            e.putLong("Amount", entry.getValue().amount());
            links.add(e);
        }
        tag.put("Links", links);
        return tag;
    }
}
