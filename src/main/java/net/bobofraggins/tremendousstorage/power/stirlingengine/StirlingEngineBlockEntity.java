package net.bobofraggins.tremendousstorage.power.stirlingengine;

import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageConfig;
import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * Block entity for the Stirling Engine generator.
 *
 * <p>Checks the block directly below each tick and generates FE based on the heat source:
 *
 * <ul>
 *   <li>Lava block → 50 FE/t
 *   <li>Magma block → 25 FE/t
 *   <li>Lit campfire → 15 FE/t
 * </ul>
 *
 * <p>Stores up to 100,000 FE internally and pushes energy to all adjacent blocks that expose an
 * {@link EnergyHandler} capability.
 *
 * <p>Client-side, {@link #animationTicks} advances while {@link #heated} is true, driving the
 * flywheel animation in {@link StirlingEngineRenderer}.
 */
public class StirlingEngineBlockEntity extends BlockEntity {

    /** Base buffer capacity at WOOD tier. Scales 4× per tier via {@link StorageTier#getScaledCapacity}. */
    public static final int BASE_MAX_ENERGY = 100_000;

    /** Base FE pushed per adjacent block per tick at WOOD tier. Scales 4× per tier. */
    private static final int BASE_PUSH_PER_TICK = 100;

    private StorageTier tier = StorageTier.WOOD;
    private int energyStored = 0;

    /** True when the block below is a recognised heat source. Synced to client. */
    private boolean heated = false;

    // ---- client-side animation ----
    public float animationTicks = 0f;

    public StirlingEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("stirling_engine"), pos, state);
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (!TremendousStorageConfig.STIRLING_ENGINE_ENABLED.get()) return;

        int generation = getHeatGeneration();
        boolean wasHeated = heated;
        heated = generation > 0;

        int maxEnergy = getMaxEnergy();
        if (heated && energyStored < maxEnergy) {
            energyStored = Math.min(maxEnergy, energyStored + generation);
        }

        pushEnergy();

        if (heated != wasHeated) {
            setChanged();
        }
    }

    public int getHeatGeneration() {
        BlockState below = level.getBlockState(worldPosition.below());
        int base;
        if (below.is(Blocks.LAVA)) base = 50;
        else if (below.is(Blocks.MAGMA_BLOCK)) base = 25;
        else if ((below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) && below.getValue(CampfireBlock.LIT))
            base = 15;
        else return 0;
        return (int) tier.getScaledCapacity(base);
    }

    private void pushEnergy() {
        if (energyStored <= 0) return;
        for (Direction dir : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(dir);
            EnergyHandler storage = level.getCapability(Capabilities.Energy.BLOCK, adjPos, dir.getOpposite());
            if (storage == null) continue;
            int toSend = Math.min(energyStored, (int) tier.getScaledCapacity(BASE_PUSH_PER_TICK));
            int accepted;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                accepted = storage.insert(toSend, tx);
                tx.commit();
            }
            energyStored -= accepted;
            if (energyStored <= 0) break;
        }
    }

    // -------------------------------------------------------------------------
    // Client tick
    // -------------------------------------------------------------------------

    public static final float CYCLE_TICKS = 40f;

    public void clientTick() {
        if (heated) {
            animationTicks += 1.0f;
            if (animationTicks >= CYCLE_TICKS) animationTicks -= CYCLE_TICKS;
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
    }

    public boolean isHeated() {
        return heated;
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getMaxEnergy() {
        return (int) tier.getScaledCapacity(BASE_MAX_ENERGY);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Tier", tier.getId());
        output.putInt("EnergyStored", energyStored);
        output.putBoolean("Heated", heated);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tier = StorageTier.fromId(input.getStringOr("Tier", ""));
        energyStored = input.getIntOr("EnergyStored", 0);
        heated = input.getBooleanOr("Heated", false);
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Tier", tier.getId());
        tag.putBoolean("Heated", heated);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
