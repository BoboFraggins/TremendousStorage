package net.bobofraggins.tremendousstorage.storage.tube;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.AttachmentType;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.InterfaceFilterContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Stores the attachment state for each of the six tube faces.
 *
 * <p>Each face may have a Storage Interface, Import Interface, or Export Interface
 * attachment. Storage Interfaces carry a {@link Priority} value for network routing.
 * Import/Export Interfaces carry a 9-slot ghost-item filter and an Accept/Reject mode.
 *
 * <p>Import Interfaces pull items from adjacent external inventories into the network.
 * Export Interfaces push items from the network into adjacent external inventories.
 * Both operate every 20 ticks, transferring up to 64 items per operation.
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

    /** Filter mode for each Import/Export Interface (false = Accept, true = Reject). */
    private final boolean[] filterMode = new boolean[6];

    /** Ghost-item filter slots for each Import/Export Interface (9 slots per face). */
    private final ItemStack[][] filterSlots = new ItemStack[6][9];

    /** Per-face tick counter for throttling Import/Export operations. */
    private final int[] tickCounter = new int[6];

    /**
     * Cached network view; {@code null} means stale and will be rebuilt on next access.
     * Only valid on the server side.
     */
    private NetworkItemHandler networkCache = null;

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.TUBE_BE_TYPE.get(), pos, state);
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
                IItemHandler neighbor =
                        level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
                if (neighbor == null) continue;

                NetworkItemHandler network = be.getNetworkView();
                if (network == null) continue;

                net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity ni =
                        network.getNetworkInterface();
                int transferAmount = (ni != null) ? ni.getAttachmentTransferAmount() : 1;
                if (type == AttachmentType.IMPORT_INTERFACE) {
                    doImport(neighbor, network, be.filterSlots[i], be.filterMode[i], transferAmount);
                } else {
                    doExport(network, neighbor, be.filterSlots[i], be.filterMode[i], transferAmount);
                }
            }
        }
    }

    /**
     * Pulls items from the external {@code source} inventory into the {@code network}.
     * Scans source slots in order; transfers the first passing stack, up to {@code transferAmount}.
     */
    private static void doImport(
            IItemHandler source,
            NetworkItemHandler network,
            ItemStack[] filter,
            boolean rejectMode,
            int transferAmount) {
        int slots = source.getSlots();
        for (int s = 0; s < slots; s++) {
            ItemStack inSlot = source.getStackInSlot(s);
            if (inSlot.isEmpty()) continue;
            if (!passesFilter(inSlot, filter, rejectMode)) continue;

            int toMove = Math.min(inSlot.getCount(), transferAmount);
            // Simulate extract
            ItemStack extracted = source.extractItem(s, toMove, true);
            if (extracted.isEmpty()) continue;
            // Simulate insert into network
            ItemStack remainder = network.insertItem(0, extracted, true);
            int canInsert = extracted.getCount() - remainder.getCount();
            if (canInsert <= 0) continue;
            // Execute
            ItemStack actualExtract = source.extractItem(s, canInsert, false);
            network.insertItem(0, actualExtract, false);
            return; // one slot per tick cycle
        }
    }

    /**
     * Pulls items from the {@code network} and pushes them into the external {@code dest} inventory.
     * Scans network slots in order; transfers the first passing stack, up to {@code transferAmount}.
     */
    private static void doExport(
            NetworkItemHandler network, IItemHandler dest, ItemStack[] filter, boolean rejectMode, int transferAmount) {
        int slots = network.getSlots();
        for (int s = 0; s < slots; s++) {
            ItemStack inSlot = network.getStackInSlot(s);
            if (inSlot.isEmpty()) continue;
            if (!passesFilter(inSlot, filter, rejectMode)) continue;

            int toMove = Math.min(inSlot.getCount(), transferAmount);
            // Simulate extract from network
            ItemStack extracted = network.extractItem(s, toMove, true);
            if (extracted.isEmpty()) continue;
            // Simulate insert into destination
            ItemStack remainder = tryInsertAll(dest, extracted, true);
            int canInsert = extracted.getCount() - remainder.getCount();
            if (canInsert <= 0) continue;
            // Execute
            ItemStack actualExtract = network.extractItem(s, canInsert, false);
            tryInsertAll(dest, actualExtract, false);
            return; // one slot per tick cycle
        }
    }

    /** Attempts to insert {@code stack} into every slot of {@code handler}. Returns remainder. */
    private static ItemStack tryInsertAll(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        int slots = handler.getSlots();
        for (int s = 0; s < slots && !remaining.isEmpty(); s++) {
            remaining = handler.insertItem(s, remaining, simulate);
        }
        if (slots == 0 && !remaining.isEmpty()) {
            remaining = handler.insertItem(0, remaining, simulate);
        }
        return remaining;
    }

    /**
     * Returns true if {@code candidate} should be transferred given the filter.
     *
     * <p>Empty Accept filter → accept all. Empty Reject filter → reject all.
     * Match is by item type only ({@link ItemStack#isSameItem}).
     */
    static boolean passesFilter(ItemStack candidate, ItemStack[] filter, boolean rejectMode) {
        boolean anyFilter = false;
        for (ItemStack f : filter) {
            if (!f.isEmpty()) {
                anyFilter = true;
                break;
            }
        }
        if (!anyFilter) return !rejectMode;
        boolean inList = false;
        for (ItemStack f : filter) {
            if (!f.isEmpty() && ItemStack.isSameItem(f, candidate)) {
                inList = true;
                break;
            }
        }
        return rejectMode ? !inList : inList;
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
            filterMode[faceIndex] = false;
            for (int s = 0; s < 9; s++) filterSlots[faceIndex][s] = ItemStack.EMPTY;
        }
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

    public boolean getFilterMode(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return false;
        return filterMode[faceIndex];
    }

    public void setFilterMode(int faceIndex, boolean rejectMode) {
        if (faceIndex < 0 || faceIndex >= 6) return;
        filterMode[faceIndex] = rejectMode;
        setChanged();
    }

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
        filterMode[faceIndex] = contents.rejectMode();
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
        return new InterfaceFilterContents(list, filterMode[faceIndex]);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Attachment types as byte[6]
        byte[] types = new byte[6];
        for (int i = 0; i < 6; i++) types[i] = (byte) attachmentType[i].ordinal();
        tag.putByteArray("AttachmentTypes", types);

        // Priorities as byte[6]
        byte[] prios = new byte[6];
        for (int i = 0; i < 6; i++) prios[i] = (byte) attachmentPriority[i].ordinal();
        tag.putByteArray("AttachmentPriorities", prios);

        // Filter modes packed into one byte (bit i = filterMode[i])
        int modeMask = 0;
        for (int i = 0; i < 6; i++) {
            if (filterMode[i]) modeMask |= (1 << i);
        }
        tag.putByte("FilterMode", (byte) modeMask);

        // Filter slots: outer ListTag of 6 inner ListTags, each with 9 CompoundTags
        ListTag outerList = new ListTag();
        for (int i = 0; i < 6; i++) {
            ListTag innerList = new ListTag();
            for (int s = 0; s < 9; s++) {
                CompoundTag slotTag = new CompoundTag();
                if (!filterSlots[i][s].isEmpty()) {
                    filterSlots[i][s].save(registries, slotTag);
                }
                innerList.add(slotTag);
            }
            outerList.add(innerList);
        }
        tag.put("FilterSlots", outerList);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Attachment types
        if (tag.contains("AttachmentTypes")) {
            byte[] types = tag.getByteArray("AttachmentTypes");
            for (int i = 0; i < 6; i++) {
                attachmentType[i] =
                        (i < types.length) ? AttachmentType.fromOrdinal(types[i] & 0xFF) : AttachmentType.NONE;
            }
        } else if (tag.contains("Attachments")) {
            // Legacy migration: old boolean bitmask
            int mask = tag.getByte("Attachments") & 0xFF;
            for (int i = 0; i < 6; i++) {
                attachmentType[i] = (mask & (1 << i)) != 0 ? AttachmentType.STORAGE_INTERFACE : AttachmentType.NONE;
            }
        }

        // Priorities
        byte[] prios = tag.getByteArray("AttachmentPriorities");
        for (int i = 0; i < 6; i++) {
            attachmentPriority[i] = (i < prios.length) ? Priority.fromOrdinal(prios[i] & 0xFF) : Priority.NORMAL;
        }

        // Filter modes
        int modeMask = tag.getByte("FilterMode") & 0xFF;
        for (int i = 0; i < 6; i++) {
            filterMode[i] = (modeMask & (1 << i)) != 0;
        }

        // Filter slots
        if (tag.contains("FilterSlots", Tag.TAG_LIST)) {
            ListTag outerList = tag.getList("FilterSlots", Tag.TAG_LIST);
            for (int i = 0; i < 6 && i < outerList.size(); i++) {
                ListTag innerList = outerList.getList(i);
                for (int s = 0; s < 9 && s < innerList.size(); s++) {
                    CompoundTag slotTag = innerList.getCompound(s);
                    filterSlots[i][s] =
                            slotTag.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, slotTag);
                }
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
