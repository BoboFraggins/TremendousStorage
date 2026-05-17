package net.bobofraggins.tremendousstorage.storage.tube;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.AttachmentType;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.InterfaceFilterContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Stores the attachment state for each of the six tube faces.
 *
 * <p>Each face may have a Storage Interface, Import Interface, or Export Interface
 * attachment. Storage Interfaces carry a {@link Priority} value for network routing.
 * Import/Export Interfaces carry a 9-slot ghost-item filter.
 *
 * <p>Import Interfaces pull items from adjacent external inventories into the network.
 * An empty import filter passes all items; a non-empty filter passes only matching items.
 * Export Interfaces push items from the network into adjacent external inventories.
 * An empty export filter blocks all items; a non-empty filter passes only matching items.
 * Both operate every 10 ticks.
 */
public class TubeBlockEntity extends BlockEntity {

    private static final int TRANSFER_INTERVAL = 10;

    /** The type of attachment on each face (indexed by Direction ordinal). */
    private final AttachmentType[] attachmentType = new AttachmentType[] {
        AttachmentType.NONE, AttachmentType.NONE, AttachmentType.NONE,
        AttachmentType.NONE, AttachmentType.NONE, AttachmentType.NONE
    };

    /** Priority for each Storage Interface attachment (default NORMAL). */
    private final Priority[] attachmentPriority = new Priority[] {
        Priority.NORMAL, Priority.NORMAL, Priority.NORMAL,
        Priority.NORMAL, Priority.NORMAL, Priority.NORMAL
    };

    /** Ghost-item filter slots for each Import/Export Interface (9 slots per face). */
    private final ItemStack[][] filterSlots = new ItemStack[6][9];

    /** Per-face tick counter for throttling Import/Export operations. */
    private final int[] tickCounter = new int[6];

    /**
     * Cached network view; {@code null} means stale and will be rebuilt on next access.
     * Only valid on the server side.
     */
    private NetworkItemHandler networkCache = null;

    /**
     * Set when this tube's connectivity or attachments change so the Network Interface can
     * detect the change on its next 5-tick poll and trigger a rescan. Cleared by the NI
     * after each successful scan.
     */
    private boolean networkDirty = false;

    /** Tier of the connected Network Interface, pushed by the NI after each scan. Synced to client for rendering. */
    private StorageTier networkTier = StorageTier.WOOD;

    public StorageTier getNetworkTier() {
        return networkTier;
    }

    /**
     * Called by the Network Interface after a scan to propagate its tier for rendering.
     * Does not clear the network cache or notify neighbors.
     */
    public boolean isNetworkDirty() {
        return networkDirty;
    }

    public void markNetworkDirty() {
        networkDirty = true;
    }

    public void clearNetworkDirty() {
        networkDirty = false;
    }

    public void setNetworkTier(StorageTier tier) {
        if (networkTier == tier) return;
        networkTier = tier;
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("tube"), pos, state);
        for (int i = 0; i < 6; i++) {
            for (int s = 0; s < 9; s++) {
                filterSlots[i][s] = ItemStack.EMPTY;
            }
        }
    }

    // -------------------------------------------------------------------------
    // onLoad — fix stale connection state from old world data
    // -------------------------------------------------------------------------

    /**
     * Called when this block entity is added to the world. Recomputes the tube's
     * connection blockstate so that stale values from old world data are corrected
     * without needing a manual block update.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) return;
        BlockState current = getBlockState();
        if (!(current.getBlock() instanceof TubeBlock tubeBlock)) return;
        BlockState corrected = tubeBlock.computeState(current, level, worldPosition);
        if (!corrected.equals(current)) {
            level.setBlockAndUpdate(worldPosition, corrected);
        }
        setChanged();
        networkDirty = true;
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    /** Called every server tick by {@link TubeBlock#getTicker}. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TubeBlockEntity be) {
        for (int i = 0; i < 6; i++) {
            AttachmentType type = be.attachmentType[i];
            if (type == AttachmentType.NONE || type == AttachmentType.STORAGE_INTERFACE) continue;

            be.tickCounter[i]++;

            if (type == AttachmentType.IMPORT_INTERFACE || type == AttachmentType.EXPORT_INTERFACE) {
                if (be.tickCounter[i] % TRANSFER_INTERVAL != 0) continue;

                Direction dir = Direction.values()[i];
                BlockPos neighborPos = pos.relative(dir);
                ResourceHandler<ItemResource> neighbor =
                        level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite());
                if (neighbor == null) continue;

                NetworkItemHandler network = be.getNetworkView();
                if (network == null) continue;

                net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity ni =
                        network.getNetworkInterface();
                int transferAmount = (ni != null) ? ni.getAttachmentTransferAmount() : 1;
                if (type == AttachmentType.IMPORT_INTERFACE) {
                    doImport(neighbor, network, be.filterSlots[i], transferAmount);
                } else {
                    doExport(network, neighbor, be.filterSlots[i], transferAmount);
                }
            }
        }
    }

    /**
     * Pulls items from the external {@code source} inventory into the {@code network}.
     * Scans source slots in order; transfers the first passing stack, up to {@code transferAmount}.
     * Empty filter passes all items.
     */
    private static void doImport(
            ResourceHandler<ItemResource> source, NetworkItemHandler network, ItemStack[] filter, int transferAmount) {
        int remaining = transferAmount;
        for (int s = 0; s < source.size() && remaining > 0; s++) {
            ItemResource res = source.getResource(s);
            if (res.isEmpty()) continue;
            ItemStack probe = res.toStack(1);
            if (!passesFilter(probe, filter, true)) continue;
            int available = (int) Math.min(source.getAmountAsLong(s), remaining);
            if (available <= 0) continue;
            int inserted = network.insert(0, res, available, null);
            if (inserted <= 0) continue;
            source.extract(s, res, inserted, null);
            remaining -= inserted;
        }
    }

    /**
     * Pulls items from the {@code network} and pushes them into the external {@code dest} inventory.
     * Scans network slots in order; transfers the first passing stack, up to {@code transferAmount}.
     * Empty filter blocks all items.
     */
    private static void doExport(
            NetworkItemHandler network, ResourceHandler<ItemResource> dest, ItemStack[] filter, int transferAmount) {
        int remaining = transferAmount;
        for (int s = 0; s < network.size() && remaining > 0; s++) {
            ItemResource res = network.getResource(s);
            if (res.isEmpty()) continue;
            ItemStack probe = res.toStack(1);
            if (!passesFilter(probe, filter, false)) continue;
            int available = (int) Math.min(network.getAmountAsLong(s), remaining);
            if (available <= 0) continue;
            int inserted = tryInsertAll(dest, res, available);
            if (inserted <= 0) break; // destination full
            network.extract(s, res, inserted, null);
            remaining -= inserted;
        }
    }

    /** Attempts to insert {@code resource} into every slot of {@code handler}. Returns total inserted. */
    private static int tryInsertAll(ResourceHandler<ItemResource> handler, ItemResource resource, int amount) {
        int remaining = amount;
        int slots = handler.size();
        for (int s = 0; s < slots && remaining > 0; s++) {
            remaining -= handler.insert(s, resource, remaining, null);
        }
        if (slots == 0 && remaining > 0) {
            remaining -= handler.insert(0, resource, remaining, null);
        }
        return amount - remaining;
    }

    /**
     * Returns true if {@code candidate} should be transferred given the filter.
     *
     * <p>If all filter slots are empty, returns {@code emptyMatchesAll} (true for importers,
     * false for exporters). Otherwise returns true only if the candidate matches a filter slot.
     * Match is by item type only ({@link ItemStack#isSameItem}).
     */
    static boolean passesFilter(ItemStack candidate, ItemStack[] filter, boolean emptyMatchesAll) {
        boolean anyFilter = false;
        for (ItemStack f : filter) {
            if (!f.isEmpty()) {
                anyFilter = true;
                if (ItemStack.isSameItem(f, candidate)) return true;
            }
        }
        return !anyFilter && emptyMatchesAll;
    }

    // -------------------------------------------------------------------------
    // Network view
    // -------------------------------------------------------------------------

    /**
     * Returns this tube's network-wide {@link NetworkItemHandler}, building it lazily
     * via BFS if the cache is stale.
     *
     * <p>Returns {@code null} on the client side or before the level is available,
     * which NeoForge treats as "capability absent".
     */
    public NetworkItemHandler getNetworkView() {
        if (level == null || level.isClientSide()) return null;
        if (networkCache == null) {
            networkCache = TubeNetwork.buildNetworkView((ServerLevel) level, worldPosition);
        }
        return networkCache;
    }

    /** Returns true if the network cache is currently populated (not stale). */
    public boolean hasNetworkCache() {
        return networkCache != null;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // Accessors — attachment type
    // -------------------------------------------------------------------------

    /** Returns true if any attachment is installed on the given face. */
    public boolean hasAttachment(int faceIndex) {
        return faceIndex >= 0 && faceIndex < 6 && attachmentType[faceIndex] != AttachmentType.NONE;
    }

    public AttachmentType getAttachmentType(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return AttachmentType.NONE;
        return attachmentType[faceIndex];
    }

    /**
     * Sets the attachment type for a face.
     * Resets priority to NORMAL and clears filter data when set to NONE.
     */
    public void setAttachmentType(int faceIndex, AttachmentType type) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        attachmentType[faceIndex] = type;
        if (type == AttachmentType.NONE) {
            attachmentPriority[faceIndex] = Priority.NORMAL;
            for (int s = 0; s < 9; s++) filterSlots[faceIndex][s] = ItemStack.EMPTY;
        }
        networkDirty = true;
        setChanged();
        // Invalidate neighbors so their stale cached network views are discarded.
        if (level != null) {
            for (Direction dir : Direction.values()) {
                level.invalidateCapabilities(worldPosition.relative(dir));
            }
        }
    }

    /** Convenience shim used by TubeBlock for Storage Interface installs/removals. */
    public void setAttachment(int faceIndex, boolean present) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        if (!present) {
            setAttachmentType(faceIndex, AttachmentType.NONE);
        } else if (attachmentType[faceIndex] == AttachmentType.NONE) {
            setAttachmentType(faceIndex, AttachmentType.STORAGE_INTERFACE);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors — priority
    // -------------------------------------------------------------------------

    public Priority getAttachmentPriority(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return Priority.NORMAL;
        return attachmentPriority[faceIndex];
    }

    public void setAttachmentPriority(int faceIndex, Priority p) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        attachmentPriority[faceIndex] = p;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Accessors — filter
    // -------------------------------------------------------------------------

    public ItemStack getFilterSlot(int faceIndex, int slot) {
        if (faceIndex < 0 || faceIndex >= 6 || slot < 0 || slot >= 9) return ItemStack.EMPTY;
        return filterSlots[faceIndex][slot];
    }

    public void setFilterSlot(int faceIndex, int slot, ItemStack stack) {
        if (faceIndex < 0 || faceIndex >= 6 || slot < 0 || slot >= 9) return;
        filterSlots[faceIndex][slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    /**
     * Loads filter data from an {@link InterfaceFilterContents} data component
     * (read from the item being installed on this face).
     */
    public void loadFilterFromContents(int faceIndex, InterfaceFilterContents contents) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        java.util.List<ItemStack> src = contents.slots();
        for (int s = 0; s < 9; s++) {
            filterSlots[faceIndex][s] =
                    (s < src.size() && !src.get(s).isEmpty()) ? src.get(s).copyWithCount(1) : ItemStack.EMPTY;
        }
        // No setChanged() here; caller is expected to call setAttachmentType which calls setChanged
    }

    /**
     * Serializes the current filter state for a face into an {@link InterfaceFilterContents}
     * suitable for storing on the dropped item's data component.
     */
    public InterfaceFilterContents saveFilterToContents(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return InterfaceFilterContents.EMPTY;
        java.util.List<ItemStack> list = new java.util.ArrayList<>(9);
        for (int s = 0; s < 9; s++) {
            list.add(
                    filterSlots[faceIndex][s].isEmpty() ? ItemStack.EMPTY : filterSlots[faceIndex][s].copyWithCount(1));
        }
        return new InterfaceFilterContents(list);
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        networkCache = null; // clear cache before capability invalidation fires
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString("NetworkTier", networkTier.getId());

        // Attachment types as int[6]
        int[] types = new int[6];
        for (int i = 0; i < 6; i++) types[i] = attachmentType[i].ordinal();
        output.putIntArray("AttachmentTypes", types);

        // Priorities as int[6]
        int[] prios = new int[6];
        for (int i = 0; i < 6; i++) prios[i] = attachmentPriority[i].ordinal();
        output.putIntArray("AttachmentPriorities", prios);

        // Filter slots as a flat list of {face, slot, item} entries
        var filterList = output.childrenList("FilterSlots");
        for (int i = 0; i < 6; i++) {
            for (int s = 0; s < 9; s++) {
                if (!filterSlots[i][s].isEmpty()) {
                    var entry = filterList.addChild();
                    entry.putInt("face", i);
                    entry.putInt("slot", s);
                    entry.store("item", ItemStack.OPTIONAL_CODEC, filterSlots[i][s]);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getString("NetworkTier").ifPresent(t -> networkTier = StorageTier.fromId(t));

        // Attachment types from int[6]
        int[] types = input.getIntArray("AttachmentTypes").orElse(new int[0]);
        for (int i = 0; i < 6; i++) {
            attachmentType[i] = (i < types.length) ? AttachmentType.fromOrdinal(types[i]) : AttachmentType.NONE;
        }

        // Priorities from int[6]
        int[] prios = input.getIntArray("AttachmentPriorities").orElse(new int[0]);
        for (int i = 0; i < 6; i++) {
            attachmentPriority[i] = (i < prios.length) ? Priority.fromOrdinal(prios[i]) : Priority.NORMAL;
        }

        // Filter slots from flat list
        for (var entry : input.childrenListOrEmpty("FilterSlots")) {
            int face = entry.getIntOr("face", -1);
            int slot = entry.getIntOr("slot", -1);
            if (face >= 0 && face < 6 && slot >= 0 && slot < 9) {
                filterSlots[face][slot] =
                        entry.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
