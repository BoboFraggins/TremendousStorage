package net.bobofraggins.intellistore.storage.networkinterface;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Implemented by every NetworkConnector block entity to cache the position of the nearest
 * reachable Network Interface.
 *
 * <p>The cache is invalidated (set to {@code null}) inside {@code setChanged()} so that any
 * topology change that propagates through the network automatically forces a fresh
 * {@link net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBFS#findNI} scan on
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
}
