package net.bobofraggins.intellistore.power.stirlingengine;

import net.bobofraggins.intellistore.shared.config.IntelliStoreConfig;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Block entity for the Stirling Engine generator.
 *
 * <p>Checks the block directly below each tick and generates FE based on the heat source:
 * <ul>
 *   <li>Lava block → 50 FE/t
 *   <li>Magma block → 25 FE/t
 *   <li>Lit campfire → 15 FE/t
 * </ul>
 *
 * <p>Stores up to 100,000 FE internally and pushes energy to all adjacent blocks
 * that expose an {@link IEnergyStorage} capability.
 *
 * <p>Client-side, {@link #animationTicks} advances while {@link #heated} is true,
 * driving the spinning flywheel animation in {@link StirlingEngineRenderer}.
 */
public class StirlingEngineBlockEntity extends BlockEntity {

    public static final int MAX_ENERGY = 100_000;
    /** Max FE pushed per adjacent block per tick. */
    private static final int PUSH_PER_TICK = 100;

    private int energyStored = 0;

    /** True when the block below is a recognised heat source. Synced to client. */
    private boolean heated = false;

    // ---- client-side animation ----
    public float animationTicks = 0f;

    public StirlingEngineBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STIRLING_ENGINE_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (!IntelliStoreConfig.STIRLING_ENGINE_ENABLED.get()) return;

        int generation = getHeatGeneration();
        boolean wasHeated = heated;
        heated = generation > 0;

        if (heated && energyStored < MAX_ENERGY) {
            energyStored = Math.min(MAX_ENERGY, energyStored + generation);
        }

        pushEnergy();

        // Sync heated state to clients when it changes
        if (heated != wasHeated) {
            setChanged();
        }
    }

    private int getHeatGeneration() {
        BlockState below = level.getBlockState(worldPosition.below());
        if (below.is(Blocks.LAVA)) return 50;
        if (below.is(Blocks.MAGMA_BLOCK)) return 25;
        if (below.is(Blocks.CAMPFIRE) && below.getValue(CampfireBlock.LIT)) return 15;
        if (below.is(Blocks.SOUL_CAMPFIRE) && below.getValue(CampfireBlock.LIT)) return 15;
        return 0;
    }

    private void pushEnergy() {
        if (energyStored <= 0) return;
        for (Direction dir : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(dir);
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, adjPos, dir.getOpposite());
            if (storage == null || !storage.canReceive()) continue;
            int toSend = Math.min(energyStored, PUSH_PER_TICK);
            int accepted = storage.receiveEnergy(toSend, false);
            energyStored -= accepted;
            if (energyStored <= 0) break;
        }
    }

    // -------------------------------------------------------------------------
    // Client tick
    // -------------------------------------------------------------------------

    public void clientTick() {
        if (heated) {
            animationTicks += 1.5f;
            if (animationTicks > 3600f) animationTicks -= 3600f; // keep small
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isHeated() {
        return heated;
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    /**
     * Receive energy from an adjacent energy source (e.g., a Pipez energy pipe).
     * Returns the amount actually accepted.
     */
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int space = MAX_ENERGY - energyStored;
        int accepted = Math.min(maxReceive, space);
        if (!simulate && accepted > 0) {
            energyStored += accepted;
            setChanged();
        }
        return accepted;
    }

    /** Called by {@link StirlingEngineEnergyHandler} to extract energy from the buffer. */
    void extractEnergy(int amount) {
        energyStored = Math.max(0, energyStored - amount);
        setChanged();
    }

    // -------------------------------------------------------------------------
    // setChanged
    // -------------------------------------------------------------------------

    @Override
    public void setChanged() {
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
        tag.putInt("EnergyStored", energyStored);
        tag.putBoolean("Heated", heated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStored = tag.getInt("EnergyStored");
        heated = tag.getBoolean("Heated");
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
