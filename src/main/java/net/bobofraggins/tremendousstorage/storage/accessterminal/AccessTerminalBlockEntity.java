package net.bobofraggins.tremendousstorage.storage.accessterminal;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiCacheHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Storage Access Terminal.
 *
 * <p>Exists solely to drive a server-side tick that keeps the {@code active} blockstate
 * property in sync with the connected Network Interface's network validity state. BFS runs
 * every 20 ticks (1 second) to avoid per-tick overhead.
 */
public class AccessTerminalBlockEntity extends BlockEntity implements NiCacheHolder {

    private int tickCounter = 0;
    private SortMode sortMode = SortMode.AMOUNT;
    private boolean hasCraftingUpgrade = false;

    @Nullable
    private BlockPos cachedNiPos = null;

    public AccessTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_ACCESS_TERMINAL_BE_TYPE.get(), pos, state);
    }

    @Override
    public void invalidateNiCache() {
        cachedNiPos = null;
    }

    @Override
    @Nullable
    public BlockPos getOrFindNiPos(ServerLevel level) {
        if (cachedNiPos != null && !(level.getBlockEntity(cachedNiPos) instanceof NetworkInterfaceBlockEntity)) {
            cachedNiPos = null;
        }
        if (cachedNiPos == null) cachedNiPos = AccessTerminalBFS.findNI(level, worldPosition);
        return cachedNiPos;
    }

    @Override
    public void setChanged() {
        invalidateNiCache();
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        setChanged();
    }

    public boolean hasCraftingUpgrade() {
        return hasCraftingUpgrade;
    }

    public void setCraftingUpgrade(boolean value) {
        hasCraftingUpgrade = value;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("SortMode", sortMode.name());
        if (hasCraftingUpgrade) tag.putBoolean("CraftingUpgrade", true);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            sortMode = SortMode.valueOf(tag.getString("SortMode"));
        } catch (IllegalArgumentException e) {
            sortMode = SortMode.AMOUNT;
        }
        hasCraftingUpgrade = tag.getBoolean("CraftingUpgrade");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
    }

    // -------------------------------------------------------------------------
    // Ticker
    // -------------------------------------------------------------------------

    /** Called each server tick by the block ticker. */
    public void serverTick() {
        if (++tickCounter < 20) return;
        tickCounter = 0;

        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos niPos = getOrFindNiPos(serverLevel);
        boolean active = niPos != null
                && level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni
                && ni.isNetworkValid();

        BlockState state = getBlockState();
        if (state.getValue(AccessTerminalBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(AccessTerminalBlock.ACTIVE, active), 3);
        }
    }
}
