package net.bobofraggins.intellistore.storage.battery;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
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
 * up to NETHERITE. Each server tick, any stored energy is actively pushed to all adjacent blocks
 * that expose an {@link IEnergyStorage} capability.
 */
public class BatteryBlockEntity extends BlockEntity {

    /** Base capacity at PAPER tier (100 000 FE). Scales 4× per tier. */
    public static final int BASE_CAPACITY = 100_000;

    /** Base FE pushed per adjacent receiver per tick at PAPER tier. Scales 4× per tier. */
    private static final int BASE_PUSH_RATE = 10_000;

    /** Client sync period in ticks (4 per second). */
    private static final int SYNC_INTERVAL = 5;

    private int energyStored = 0;
    private StorageTier tier = StorageTier.PAPER;

    // Used to skip client syncs when nothing changed
    private int lastSyncEnergy = -1;
    private int syncCooldown = 0;

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
        be.pushEnergy();
        be.syncCooldown--;
        if (be.syncCooldown <= 0 && be.energyStored != be.lastSyncEnergy) {
            be.lastSyncEnergy = be.energyStored;
            be.syncCooldown = SYNC_INTERVAL;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void pushEnergy() {
        if (level == null || energyStored <= 0) return;
        int maxPush = (int) tier.getScaledCapacity(BASE_PUSH_RATE);
        for (Direction dir : Direction.values()) {
            if (energyStored <= 0) break;
            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
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
