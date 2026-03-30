package net.bobofraggins.intellistore.storage.battery;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkScanResult;
import net.bobofraggins.intellistore.storage.tube.NetworkItemHandler;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.bobofraggins.intellistore.storage.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Block entity for the Battery.
 *
 * <p>Stores FE with a base capacity of {@link #BASE_CAPACITY} at PAPER tier, scaling 4× per tier
 * up to NETHERITE. Each server tick the battery pulls energy from directly adjacent sources and
 * pushes to directly adjacent non-tube, non-battery consumers. Every {@link
 * #NETWORK_INTERVAL} ticks it also walks the connected tube network: first distributing its
 * stored energy to every power-hungry machine on the network (skipping other batteries), then
 * topping itself off by pulling from any generators it finds on the network.
 */
public class BatteryBlockEntity extends BlockEntity {

    /** Base capacity at PAPER tier (100 000 FE). Scales 4× per tier. */
    public static final int BASE_CAPACITY = 100_000;

    /** Base FE pushed per receiver per tick at PAPER tier. Scales 4× per tier. */
    private static final int BASE_PUSH_RATE = 10_000;

    /** Client sync period in ticks (4 per second). */
    private static final int SYNC_INTERVAL = 5;

    /** How often (in ticks) the battery performs network-wide distribution. */
    private static final int NETWORK_INTERVAL = 5;

    private int energyStored = 0;
    private StorageTier tier = StorageTier.PAPER;

    // Used to skip client syncs when nothing changed
    private int lastSyncEnergy = -1;
    private int syncCooldown = 0;
    private int networkCooldown = 0;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.BATTERY_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        energyStored = Math.min(energyStored, getMaxEnergy());
        setChanged();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getMaxEnergy() {
        return (int) tier.getScaledCapacity(BASE_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // Energy operations (delegated from BatteryEnergyHandler)
    // -------------------------------------------------------------------------

    int receiveEnergy(int maxReceive, boolean simulate) {
        int accepted = Math.min(maxReceive, getMaxEnergy() - energyStored);
        if (!simulate && accepted > 0) {
            energyStored += accepted;
            setChanged();
        }
        return accepted;
    }

    int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.min(maxExtract, energyStored);
        if (!simulate && extracted > 0) {
            energyStored -= extracted;
            setChanged();
        }
        return extracted;
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        be.pullEnergy();
        be.pushEnergy();

        be.networkCooldown--;
        if (be.networkCooldown <= 0) {
            be.networkCooldown = NETWORK_INTERVAL;
            NetworkInterfaceBlockEntity ni = be.resolveNi();
            if (ni != null) {
                be.distributeToNetworkSinks(ni);
                be.chargeFromNetworkSources(ni);
            }
        }

        be.syncCooldown--;
        if (be.syncCooldown <= 0 && be.energyStored != be.lastSyncEnergy) {
            be.lastSyncEnergy = be.energyStored;
            be.syncCooldown = SYNC_INTERVAL;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // -------------------------------------------------------------------------
    // Per-tick adjacent pull/push
    // -------------------------------------------------------------------------

    private void pullEnergy() {
        if (level == null || energyStored >= getMaxEnergy()) return;
        int maxPull = (int) tier.getScaledCapacity(BASE_PUSH_RATE);
        for (Direction dir : Direction.values()) {
            if (energyStored >= getMaxEnergy()) break;
            IEnergyStorage source = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (source == null || !source.canExtract()) continue;
            int toRequest = Math.min(getMaxEnergy() - energyStored, maxPull);
            int received = source.extractEnergy(toRequest, false);
            if (received > 0) {
                energyStored += received;
                setChanged();
            }
        }
    }

    private void pushEnergy() {
        if (level == null || energyStored <= 0) return;
        int maxPush = (int) tier.getScaledCapacity(BASE_PUSH_RATE);
        for (Direction dir : Direction.values()) {
            if (energyStored <= 0) break;
            BlockPos neighborPos = worldPosition.relative(dir);
            if (level.getBlockState(neighborPos).getBlock() instanceof TubeBlock) continue;
            if (level.getBlockEntity(neighborPos) instanceof BatteryBlockEntity) continue;
            IEnergyStorage target =
                    level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (target == null || !target.canReceive()) continue;
            int toSend = Math.min(energyStored, maxPush);
            int accepted = target.receiveEnergy(toSend, false);
            if (accepted > 0) {
                energyStored -= accepted;
                setChanged();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Periodic network-wide distribution
    // -------------------------------------------------------------------------

    /**
     * Finds the Network Interface for the tube network this battery is connected to, or
     * {@code null} if the battery is not adjacent to any tube.
     */
    private NetworkInterfaceBlockEntity resolveNi() {
        if (level == null) return null;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (level.getBlockState(neighborPos).getBlock() instanceof TubeBlock
                    && level.getBlockEntity(neighborPos) instanceof TubeBlockEntity tubeBE) {
                NetworkItemHandler network = tubeBE.getNetworkView();
                if (network != null) return network.getNetworkInterface();
            }
        }
        return null;
    }

    /**
     * Walks every tube in the network and pushes energy to any adjacent block that can receive
     * and is not already full. Other batteries are skipped — they charge themselves.
     */
    private void distributeToNetworkSinks(NetworkInterfaceBlockEntity ni) {
        if (energyStored <= 0) return;
        NetworkScanResult scan = ni.getScan();
        if (scan == null) return;
        int maxPush = (int) tier.getScaledCapacity(BASE_PUSH_RATE);
        outer:
        for (BlockPos tubePos : scan.tubePositions()) {
            for (Direction dir : Direction.values()) {
                if (energyStored <= 0) break outer;
                BlockPos adjPos = tubePos.relative(dir);
                if (adjPos.equals(worldPosition)) continue;
                if (level.getBlockState(adjPos).getBlock() instanceof TubeBlock) continue;
                if (level.getBlockEntity(adjPos) instanceof BatteryBlockEntity) continue;
                IEnergyStorage target =
                        level.getCapability(Capabilities.EnergyStorage.BLOCK, adjPos, dir.getOpposite());
                if (target == null || !target.canReceive()) continue;
                if (target.getEnergyStored() >= target.getMaxEnergyStored()) continue;
                int toSend = Math.min(energyStored, maxPush);
                int accepted = target.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    energyStored -= accepted;
                    setChanged();
                }
            }
        }
    }

    /**
     * Walks every tube in the network and pulls energy from any adjacent block that can extract
     * (e.g. generators), until the battery is full.
     */
    private void chargeFromNetworkSources(NetworkInterfaceBlockEntity ni) {
        if (energyStored >= getMaxEnergy()) return;
        NetworkScanResult scan = ni.getScan();
        if (scan == null) return;
        int maxPull = (int) tier.getScaledCapacity(BASE_PUSH_RATE);
        outer:
        for (BlockPos tubePos : scan.tubePositions()) {
            for (Direction dir : Direction.values()) {
                if (energyStored >= getMaxEnergy()) break outer;
                BlockPos adjPos = tubePos.relative(dir);
                if (adjPos.equals(worldPosition)) continue;
                if (level.getBlockState(adjPos).getBlock() instanceof TubeBlock) continue;
                IEnergyStorage source =
                        level.getCapability(Capabilities.EnergyStorage.BLOCK, adjPos, dir.getOpposite());
                if (source == null || !source.canExtract()) continue;
                int toRequest = Math.min(getMaxEnergy() - energyStored, maxPull);
                int received = source.extractEnergy(toRequest, false);
                if (received > 0) {
                    energyStored += received;
                    setChanged();
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
        tag.putString("Tier", tier.getId());
        tag.putInt("EnergyStored", energyStored);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = StorageTier.fromId(tag.getString("Tier"));
        energyStored = Math.min(tag.getInt("EnergyStored"), getMaxEnergy());
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
