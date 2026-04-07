package net.bobofraggins.tremendousstorage.storage.networkinterface;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Implemented by every NetworkConnector block entity to cache the position of the nearest
 * reachable Network Interface.
 *
 * <p>The cache is invalidated (set to {@code null}) inside {@code setChanged()} so that any
 * topology change that propagates through the network automatically forces a fresh
 * {@link net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBFS#findNI} scan on
 * the next query.
 */
public interface NiCacheHolder {

    /**
     * Returns the cached NI position if still valid, otherwise runs a BFS to find it, caches
     * the result, and returns it. Returns {@code null} if no NI is reachable.
     */
    @Nullable
    BlockPos getOrFindNiPos(ServerLevel level);

    /** Clears the cached NI position. Called from {@code setChanged()}. */
    void invalidateNiCache();

    /**
     * Notifies the connected Network Interface that this block's item contents changed, without
     * triggering a full topology re-scan.
     *
     * <p>Obtains the NI via {@link #getOrFindNiPos} and calls
     * {@link NetworkInterfaceBlockEntity#markContentsDirty()} on it. This is a no-op if no NI
     * is reachable.
     */
    default void notifyNiContentsChanged(ServerLevel level) {
        BlockPos niPos = getOrFindNiPos(level);
        if (niPos == null) return;
        if (level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni) {
            ni.markContentsDirty();
        }
    }
}
