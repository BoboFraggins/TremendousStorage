package net.bobofraggins.intellistore.storage.networkinterface;

import net.bobofraggins.intellistore.shared.register.Registration;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Block entity for the lower half of a Network Interface block.
 *
 * <p>Holds a lazily-built {@link NetworkScanResult} that describes all storage blocks
 * reachable through the connected tube network (all colors). The cache is invalidated
 * whenever {@link #setChanged()} is called (which happens on neighbour changes).
 *
 * <p>Exposes an {@link IItemHandler} capability via {@link NiItemHandler}: insertion uses
 * the highest-priority storage first; extraction drains the lowest-priority storage first.
 *
 * <p><b>Power system:</b> The Network Interface stores FE energy and drains it each server
 * tick proportional to network demand (NI itself, connected SATs, Wireless Hubs, and tube
 * attachments). When the energy buffer runs out the network becomes inactive and most
 * functionality stops working. Energy can be supplied by:
 * <ul>
 *   <li>The Stirling Generator block (pushes energy into adjacent IEnergyStorage)
 *   <li>Any energy pipe (Pipez, Mekanism cables, etc.) adjacent to the NI or any tube in
 *       the network — tubes expose the NI's energy buffer as a shared capability
 * </ul>
 */
public class NetworkInterfaceBlockEntity extends BlockEntity implements MenuProvider {

    // ---- Power constants ----
    public static final int MAX_ENERGY = 100_000;
    /** FE/t consumed by this NI itself. */
    public static final int NI_COST = 5;

    /** Lazily built; {@code null} = stale. */
    private NetworkScanResult cachedScan = null;
    /** Lazily built alongside {@link #cachedScan}; {@code null} = stale. */
    private NiItemHandler cachedHandler = null;

    private int energyStored = 0;
    /** Whether the network had enough power last tick. Synced to clients. */
    private boolean powered = false;
    /** Total FE/t consumed by the network, computed during scan. Synced to clients. */
    private int totalConsumption = 0;

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
        if (cachedScan == null) {
            cachedScan = NetworkInterfaceBFS.scan((ServerLevel) level, worldPosition);
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
     * Insert = highest-priority first; extract = lowest-priority first.
     */
    public IItemHandler getItemHandler() {
        if (getScan() == null) return null;
        if (cachedHandler == null) {
            cachedHandler = new NiItemHandler(cachedScan.insertOrder());
        }
        return cachedHandler;
    }

    // -------------------------------------------------------------------------
    // Power system
    // -------------------------------------------------------------------------

    /** Returns how much energy is stored in the NI's buffer. */
    public int getEnergyStored() {
        return energyStored;
    }

    /** Returns the max energy capacity. */
    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    /**
     * Returns true if the network is currently powered (had enough energy last tick).
     * Returns true when not enough data is available (graceful before first tick).
     */
    public boolean isPowered() {
        return powered;
    }

    /** Total FE/t consumed by all network components. */
    public int getTotalConsumption() {
        return totalConsumption;
    }

    /**
     * Called each server tick by {@link NetworkInterfaceBlock#getTicker()}.
     * Drains energy for all active network components. If insufficient energy is present,
     * sets {@code powered = false} (the network becomes inactive).
     */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        NetworkScanResult scan = getScan();
        if (scan == null || !scan.isValid()) {
            // No valid network — still count base NI cost but mark unpowered
            int cost = NI_COST;
            boolean wasPowered = powered;
            powered = energyStored >= cost;
            if (powered) energyStored -= cost;
            totalConsumption = cost;
            if (powered != wasPowered) setChanged();
            return;
        }

        int cost = scan.totalFePerTick();
        boolean wasPowered = powered;
        boolean nowPowered = energyStored >= cost;
        if (nowPowered) {
            energyStored -= cost;
        }
        powered = nowPowered;
        totalConsumption = cost;

        if (powered != wasPowered) {
            setChanged();
        }
    }

    /**
     * Receives energy into the NI's buffer. Called by external energy sources
     * (Stirling Engine push, Pipez pipe injection, Mekanism cable injection, etc.).
     * Returns the amount actually accepted.
     */
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int space = MAX_ENERGY - energyStored;
        int accepted = Math.min(maxReceive, space);
        if (!simulate && accepted > 0) {
            energyStored += accepted;
            // Don't call setChanged here to avoid scan invalidation on every energy tick
            super.setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        return accepted;
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
        cachedScan = null; // invalidate before capability notification fires
        cachedHandler = null;
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.intellistore.network_interface");
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
        tag.putInt("EnergyStored", energyStored);
        tag.putBoolean("Powered", powered);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStored = tag.getInt("EnergyStored");
        powered = tag.getBoolean("Powered");
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
