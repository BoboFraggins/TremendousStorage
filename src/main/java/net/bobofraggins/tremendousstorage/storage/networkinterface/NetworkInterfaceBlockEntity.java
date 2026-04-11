package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.Set;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.IKeyCounterContributor;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.tank.TankItemAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Block entity for the Network Interface block.
 *
 * <p>Holds a lazily-built {@link NetworkScanResult} that describes all storage blocks
 * reachable through the connected tube network (all colors). The cache is invalidated
 * whenever {@link #setChanged()} is called (which happens on neighbour changes).
 *
 * <p>Exposes an {@link IItemHandler} capability via {@link NiItemHandler}: insertion uses
 * the highest-priority storage first; extraction drains the lowest-priority storage first.
 */
public class NetworkInterfaceBlockEntity extends BlockEntity implements MenuProvider {

    /** Lazily built; {@code null} = stale (topology changed). */
    private NetworkScanResult cachedScan = null;
    /** Lazily built alongside {@link #cachedScan}; {@code null} = stale. */
    private NiItemHandler cachedHandler = null;
    /** Re-entrancy guard: true while a BFS scan is in progress. */
    private boolean scanning = false;
    /**
     * True when a storage block's item contents changed but the network topology is intact.
     * Set by {@link #markContentsDirty()}; cleared when the KeyCounter is rebuilt.
     */
    private boolean contentsDirty = false;

    /** Aggregate cache of all items in the network. Null until first rebuild. */
    private KeyCounter cachedAvailableStacks = null;
    /** Monotonically increasing counter; incremented each time the cache is rebuilt. */
    private long cacheRevision = 0;

    private StorageTier tier = StorageTier.WOOD;

    public NetworkInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.NETWORK_INTERFACE_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Network scan
    // -------------------------------------------------------------------------

    /**
     * Returns the cached scan result, rebuilding it via BFS if stale.
     * Returns {@code null} on the client side or before the level is available.
     */
    public NetworkScanResult getScan() {
        if (level == null || level.isClientSide()) return null;
        if (scanning) return null; // re-entrancy guard: BFS triggered a capability query back into us
        if (cachedScan == null) {
            scanning = true;
            try {
                cachedScan = NetworkInterfaceBFS.scan((ServerLevel) level, worldPosition);
            } finally {
                scanning = false;
            }
        }
        return cachedScan;
    }

    /** Returns {@code true} if exactly one Network Interface is present on the connected network. */
    public boolean isNetworkValid() {
        NetworkScanResult s = getScan();
        return s != null && s.isValid();
    }

    // -------------------------------------------------------------------------
    // IItemHandler capability
    // -------------------------------------------------------------------------

    /**
     * Returns the network's composite item handler, or {@code null} if the scan is unavailable.
     * Insert = highest-priority first (two-phase); extract = by insert order slot index.
     */
    public IItemHandler getItemHandler() {
        if (getScan() == null) return null;
        if (cachedHandler == null) {
            cachedHandler = new NiItemHandler(cachedScan.insertOrder(), cachedScan.insertBuckets());
        }
        return cachedHandler;
    }

    // -------------------------------------------------------------------------
    // Fluid storage keys
    // -------------------------------------------------------------------------

    /**
     * Returns the set of StorageKeys that are backed by at least one TankItemAdapter
     * in the current network scan. Used when building SatContentsPacket to tag fluid entries.
     */
    public Set<StorageKey> getFluidStorageKeys() {
        NetworkScanResult scan = getScan();
        if (scan == null) return Set.of();
        KeyCounter tmp = new KeyCounter();
        for (IItemHandler h : scan.insertOrder()) {
            if (h instanceof TankItemAdapter adapter) {
                adapter.contributeToKeyCounter(tmp);
            }
        }
        Set<StorageKey> keys = new java.util.HashSet<>();
        tmp.allEntries().forEach(e -> keys.add(e.getKey()));
        return keys;
    }

    // -------------------------------------------------------------------------
    // Aggregate item cache
    // -------------------------------------------------------------------------

    /**
     * Returns the cached aggregate inventory, rebuilding lazily if stale or null.
     * Returns null on the client side or before the level is available.
     */
    public KeyCounter getCachedInventory() {
        if (level == null || level.isClientSide()) return null;
        if (cachedAvailableStacks == null || contentsDirty) {
            rebuildCache();
        }
        return cachedAvailableStacks;
    }

    /** Returns a monotonic counter that increments each time the cache is rebuilt. */
    public long getCacheRevision() {
        return cacheRevision;
    }

    private void rebuildCache() {
        NetworkScanResult scan = getScan();
        KeyCounter fresh = new KeyCounter();
        if (scan != null) {
            for (IItemHandler handler : scan.insertOrder()) {
                if (handler instanceof IKeyCounterContributor contributor) {
                    contributor.contributeToKeyCounter(fresh);
                } else {
                    int slots = handler.getSlots();
                    for (int s = 0; s < slots; s++) {
                        ItemStack stack = handler.getStackInSlot(s);
                        if (stack.isEmpty()) continue;
                        fresh.add(StorageKey.of(stack), stack.getCount());
                    }
                }
            }
        }
        cachedAvailableStacks = fresh;
        contentsDirty = false;
        cacheRevision++;
    }

    // -------------------------------------------------------------------------
    // Tier
    // -------------------------------------------------------------------------

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
    }

    /** Items transferred per import/export operation (1 at WOOD, doubles each tier). */
    public int getAttachmentTransferAmount() {
        return 1 << tier.ordinal();
    }

    /**
     * Called each server tick by {@link NetworkInterfaceBlock#getTicker()}.
     * Rebuilds the aggregate item cache if contents have changed.
     */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (contentsDirty) {
            rebuildCache();
        }
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        cachedScan = null; // invalidate before capability notification fires
        cachedHandler = null;
        cachedAvailableStacks = null; // topology change — full rebuild needed
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Marks this NI's item contents as stale without invalidating the topology scan.
     *
     * <p>Called by connected storage blocks after item insertions/extractions. Unlike
     * {@link #setChanged()}, this does <em>not</em> null {@link #cachedScan} or trigger a BFS
     * re-scan — the network topology is unchanged.
     */
    public void markContentsDirty() {
        contentsDirty = true;
    }

    /** Returns true if a storage block's item contents have changed since the last rebuild. */
    public boolean isContentsDirty() {
        return contentsDirty;
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        Component base = Component.translatable("screen.tremendousstorage.network_interface");
        if (tier == StorageTier.WOOD) return base;
        String label =
                Character.toUpperCase(tier.getId().charAt(0)) + tier.getId().substring(1);
        return base.copy().append(Component.literal(" (" + label + ")"));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (isNetworkValid() ? 1 : 0);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new NetworkInterfaceMenu(id, inv, worldPosition, data);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Tier", tier.getId());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = StorageTier.fromId(tag.getString("Tier"));
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
