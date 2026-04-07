package net.bobofraggins.tremendousstorage.storage.networkinterface;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBFS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Composable helper that block entities hold as a field to manage their NI position cache.
 *
 * <p>Encapsulates the cached NI position, cache invalidation, BFS-based lookup, and the
 * lightweight "contents changed" notification pattern — eliminating duplication across all
 * storage block entities that participate in the Network Interface system.
 */
public final class NiLink {

    @Nullable
    private BlockPos cachedPos = null;

    /** Clears the cached NI position so the next query re-runs BFS. */
    public void invalidate() {
        cachedPos = null;
    }

    /**
     * Returns the cached NI position if still valid; otherwise re-runs BFS from {@code self}.
     *
     * @return the NI position, or {@code null} if none is reachable
     */
    @Nullable
    public BlockPos getOrFindNiPos(ServerLevel level, BlockPos self) {
        if (cachedPos != null && !(level.getBlockEntity(cachedPos) instanceof NetworkInterfaceBlockEntity)) {
            cachedPos = null;
        }
        if (cachedPos == null) {
            cachedPos = AccessTerminalBFS.findNI(level, self);
        }
        return cachedPos;
    }

    /**
     * Notifies the connected NI that contents changed and syncs the block to clients.
     *
     * <p>Finds the NI via {@link #getOrFindNiPos} and calls
     * {@link NetworkInterfaceBlockEntity#markContentsDirty()} on it, then calls
     * {@link ServerLevel#sendBlockUpdated} to propagate the change to clients.
     */
    public void notifyChanged(ServerLevel level, BlockPos self, BlockState state) {
        BlockPos niPos = getOrFindNiPos(level, self);
        if (niPos != null && level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
            ni.markContentsDirty();
        }
        level.sendBlockUpdated(self, state, state, 3);
    }
}
