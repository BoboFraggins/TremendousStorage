package net.bobofraggins.intellistore.storage.accessterminal;

import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storage.networkinterface.NiCacheHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Storage Access Terminal.
 *
 * <p>Exists solely to drive a server-side tick that keeps the {@code active} blockstate
 * property in sync with the connected Network Interface's power state. BFS runs every
 * 20 ticks (1 second) to avoid per-tick overhead.
 */
public class AccessTerminalBlockEntity extends BlockEntity implements NiCacheHolder {

    private int tickCounter = 0;

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

    /** Called each server tick by the block ticker. */
    public void serverTick() {
        if (++tickCounter < 20) return;
        tickCounter = 0;

        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos niPos = getOrFindNiPos(serverLevel);
        boolean powered = niPos != null
                && level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni
                && ni.isPowered();

        BlockState state = getBlockState();
        if (state.getValue(AccessTerminalBlock.ACTIVE) != powered) {
            level.setBlock(worldPosition, state.setValue(AccessTerminalBlock.ACTIVE, powered), 3);
        }
    }
}
