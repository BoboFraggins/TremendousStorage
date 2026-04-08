package net.bobofraggins.tremendousstorage.storage.endertank;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tremendoustank.TremendousTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Block entity for the Ender Tremendous Tank.
 *
 * <p>Extends {@link TremendousTankBlockEntity} and adds shared-fluid behaviour: all block entities
 * that share the same {@code linkId} read and write from a single {@link EnderTankStorage} entry.
 *
 * <p><b>Sync lifecycle:</b>
 * <ol>
 *   <li>On load: restores local state from NBT (inherited) and reads the linkId.
 *       Sets {@code needsStorageLoad = true}.
 *   <li>First server tick: overwrites local fluid state with the authoritative storage entry,
 *       or seeds storage with the current state if this is the first linked tank placed.
 *   <li>On {@link #insert} / {@link #extract} / {@link #clearFluid}: inherited logic runs,
 *       then the updated state is written back to {@link EnderTankStorage}.
 * </ol>
 */
public class EnderTremendousTankBlockEntity extends TremendousTankBlockEntity {

    public static final String TAG_LINK_ID = "LinkId";

    private long linkId = -1L;
    private boolean needsStorageLoad = false;
    private long lastKnownVersion = -1L;

    public EnderTremendousTankBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ENDER_TREMENDOUS_TANK_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Link ID
    // -------------------------------------------------------------------------

    public long getLinkId() {
        return linkId;
    }

    // -------------------------------------------------------------------------
    // Storage sync
    // -------------------------------------------------------------------------

    @Override
    public void setTier(StorageTier tier) {
        super.setTier(tier);
        if (linkId != -1L && level != null && !level.isClientSide()) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                EnderTankStorage.get(server).setTier(linkId, tier);
            }
        }
    }

    private void loadFromStorage() {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        EnderTankStorage storage = EnderTankStorage.get(server);
        if (storage.hasLink(linkId)) {
            // Overwrite local fluid state with authoritative storage
            FluidStack type = storage.getStoredFluid(linkId);
            long amount = storage.getAmount(linkId);
            forceSetFluidState(type, amount);
            // Sync tier: push higher tier to storage (handles smithing upgrade), or take storage tier
            StorageTier storageTier = storage.getTier(linkId);
            StorageTier localTier = getTier();
            if (localTier.ordinal() > storageTier.ordinal()) {
                storage.setTier(linkId, localTier);
            } else {
                super.setTier(storageTier); // apply without re-triggering setTier override
            }
        } else {
            // First tank of this pair to be placed — seed storage with our fluid state and tier
            storage.initLink(linkId, getStoredFluid(), getAmount(), getTier());
        }
        lastKnownVersion = storage.getVersion(linkId);
    }

    /** Sets fluid state directly without triggering another syncToStorage. */
    private void forceSetFluidState(FluidStack type, long amount) {
        // We access the parent's state via its public API:
        // clearFluid() on the parent zeros everything, then we re-insert if there is fluid.
        // But we must not call our overridden clearFluid() or insert() here (infinite loop risk).
        // Instead we delegate to super's versions using the inherited insert pathway but we
        // need to handle the case where clearFluid clears the lock. Call super methods directly.
        super.clearFluid();
        if (!type.isEmpty() && amount > 0) {
            super.insert(type, amount, false);
        }
    }

    private void syncToStorage() {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        EnderTankStorage storage = EnderTankStorage.get(server);
        storage.setState(linkId, getStoredFluid(), getAmount());
        lastKnownVersion = storage.getVersion(linkId);
    }

    // -------------------------------------------------------------------------
    // Overrides — insert/extract/clear sync to storage after the real operation
    // -------------------------------------------------------------------------

    @Override
    public long insert(FluidStack fluid, long requested, boolean simulate) {
        long result = super.insert(fluid, requested, simulate);
        if (!simulate) syncToStorage();
        return result;
    }

    @Override
    public FluidStack extract(long requested, boolean simulate) {
        FluidStack result = super.extract(requested, simulate);
        if (!simulate) syncToStorage();
        return result;
    }

    @Override
    public void clearFluid() {
        super.clearFluid();
        syncToStorage();
    }

    // -------------------------------------------------------------------------
    // Server tick — first-tick storage load
    // -------------------------------------------------------------------------

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, EnderTremendousTankBlockEntity be) {
        if (be.needsStorageLoad) {
            be.needsStorageLoad = false;
            be.loadFromStorage();
        } else if (be.linkId != -1L) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                long storageVersion = EnderTankStorage.get(server).getVersion(be.linkId);
                if (storageVersion != be.lastKnownVersion) {
                    be.loadFromStorage();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkId != -1L) {
            tag.putLong(TAG_LINK_ID, linkId);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_LINK_ID)) {
            linkId = tag.getLong(TAG_LINK_ID);
            needsStorageLoad = true;
        }
    }
}
