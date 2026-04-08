package net.bobofraggins.tremendousstorage.storage.enderchest;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Ender Tremendous Chest.
 *
 * <p>Extends {@link TremendousChestBlockEntity} and adds shared-inventory behaviour: all block
 * entities that share the same {@code linkId} read and write from a single
 * {@link EnderChestStorage} entry so their contents are always in sync.
 *
 * <p><b>Sync lifecycle:</b>
 * <ol>
 *   <li>On load ({@link #loadAdditional}): restores inventory from NBT (inherited) and reads
 *       the linkId. Sets {@code needsStorageLoad = true}.
 *   <li>First server tick: if the storage already has an entry for this linkId the local
 *       inventory is overwritten with the authoritative data; otherwise the current (freshly
 *       placed) inventory is copied into storage as the initial state.
 *   <li>On {@link #insert} / {@link #extract}: the local inventory is updated (inherited),
 *       then immediately written back to storage.
 *   <li>On {@link #createMenu}: syncs from storage before the UI is shown so the player
 *       always sees the latest contents even if the other linked chest was modified recently.
 * </ol>
 */
public class EnderTremendousChestBlockEntity extends TremendousChestBlockEntity {

    public static final String TAG_LINK_ID = "LinkId";

    private long linkId = -1L;
    private boolean needsStorageLoad = false;
    protected long lastKnownVersion = -1L;

    public EnderTremendousChestBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), pos, state);
    }

    protected EnderTremendousChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Link ID
    // -------------------------------------------------------------------------

    public long getLinkId() {
        return linkId;
    }

    public void setLinkId(long id) {
        this.linkId = id;
    }

    // -------------------------------------------------------------------------
    // Storage sync helpers
    // -------------------------------------------------------------------------

    /** Reads from EnderChestStorage and overwrites the local in-memory inventory. */
    protected void loadFromStorage() {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        EnderChestStorage storage = getStorage(server);
        if (storage.hasLink(linkId)) {
            loadTypes(storage.getTypes(linkId), level.registryAccess());
        } else {
            // First chest of this pair to be placed — seed the storage with our inventory.
            storage.initLink(linkId, saveTypes(level.registryAccess()));
        }
        lastKnownVersion = storage.getVersion(linkId);
    }

    /** Writes the local in-memory inventory back to EnderChestStorage. */
    protected void syncToStorage() {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        EnderChestStorage storage = getStorage(server);
        storage.setTypes(linkId, saveTypes(level.registryAccess()));
        lastKnownVersion = storage.getVersion(linkId);
    }

    protected EnderChestStorage getStorage(MinecraftServer server) {
        return EnderChestStorage.get(server);
    }

    /**
     * Returns the current version counter from the backing storage for this link ID.
     * Overridden by subclasses that use a different storage backend (e.g. backpack).
     */
    protected long getStorageVersion() {
        if (linkId == -1L || level == null || level.isClientSide()) return lastKnownVersion;
        MinecraftServer server = level.getServer();
        return server == null ? lastKnownVersion : getStorage(server).getVersion(linkId);
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @Override
    public long insert(ItemStack type, long amount, boolean simulate) {
        long remainder = super.insert(type, amount, simulate);
        if (!simulate) syncToStorage();
        return remainder;
    }

    @Override
    public ItemStack extract(int index, long amount, boolean simulate) {
        ItemStack result = super.extract(index, amount, simulate);
        if (!simulate) syncToStorage();
        return result;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // Sync from storage so the UI shows the latest shared inventory.
        loadFromStorage();
        ContainerData data = new SimpleContainerData(1);
        return new net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestMenu(
                id, inv, worldPosition, data, hasCraftingUpgrade());
    }

    @Override
    public Component getDisplayName() {
        Component base = Component.translatable("block.tremendousstorage.tremendous_chest");
        StorageTier t = getTier();
        if (t == StorageTier.WOOD) {
            return Component.empty().append(base).append(" (Ender)");
        }
        String label = Character.toUpperCase(t.getId().charAt(0)) + t.getId().substring(1);
        return Component.empty().append(base).append(" (" + label + "/Ender)");
    }

    // -------------------------------------------------------------------------
    // Tick — deferred storage load on first server tick
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderTremendousChestBlockEntity be) {
        TremendousChestBlockEntity.serverTick(level, pos, state, be);
        if (be.needsStorageLoad) {
            be.needsStorageLoad = false;
            be.loadFromStorage();
        } else {
            long storageVersion = be.getStorageVersion();
            if (storageVersion != be.lastKnownVersion) {
                be.loadFromStorage();
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EnderTremendousChestBlockEntity be) {
        TremendousChestBlockEntity.clientTick(level, pos, state, be);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(TAG_LINK_ID, linkId);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkId = tag.getLong(TAG_LINK_ID);
        needsStorageLoad = (linkId != -1L);
    }
}
