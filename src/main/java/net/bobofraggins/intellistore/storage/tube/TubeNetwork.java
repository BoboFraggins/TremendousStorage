package net.bobofraggins.intellistore.storage.tube;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Utility class that builds a {@link NetworkItemHandler} for a tube network.
 *
 * <p>A tube network is the connected component of all {@link TubeBlock} positions reachable
 * from a starting position through same-color tube connections. {@link NetworkConnector} blocks
 * (Filing Cabinet, Junk Drawer, Bulk Storage Container, SAT, Wireless Hub, Stirling Engine)
 * act as color-agnostic bridges: their {@code IItemHandler} is collected as a storage endpoint
 * and the BFS continues through all of their adjacent tubes (any color), allowing different-
 * colored tube runs to share a single network.
 *
 * <p>This class is stateless. Call {@link #buildNetworkView} on demand; cache the result in
 * the calling {@link TubeBlockEntity} and invalidate when the topology changes.
 */
public final class TubeNetwork {

    private TubeNetwork() {}

    /**
     * Flood-fills the tube network starting at {@code start} and returns a
     * {@link NetworkItemHandler} wrapping all connected storage handlers, sorted
     * highest-priority first.
     *
     * @param level  the server-side level
     * @param start  position of the tube initiating the query
     * @param color  color of the starting tube (same-color tubes are traversed directly;
     *               different-color tubes may be reached via {@link NetworkConnector} bridges)
     * @return composite handler for the entire network; never null
     */
    public static NetworkItemHandler buildNetworkView(ServerLevel level, BlockPos start, DyeColor color) {

        Set<BlockPos> visitedTubes = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> collectedStorage = new HashSet<>();
        // Connector blocks that have already been traversed (to prevent looping back)
        Set<BlockPos> visitedConnectors = new HashSet<>();
        List<HandlerEntry> entries = new ArrayList<>();
        NetworkInterfaceBlockEntity foundNi = null;

        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visitedTubes.add(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TubeBlock tb)) continue;
            // Accept any tube color — we may arrive here via a connector bridge
            DyeColor tubeColor = tb.getColor();

            TubeBlockEntity tubeBE = level.getBlockEntity(pos) instanceof TubeBlockEntity tbe ? tbe : null;

            for (Direction dir : Direction.values()) {
                // Only follow active connections (tube arm extends toward this face)
                if (!state.getValue(TubeBlock.DIR_PROPS[dir.ordinal()])) continue;

                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof TubeBlock neighborTube
                        && neighborTube.getColor() == tubeColor) {
                    // Same-color tube: extend BFS directly
                    if (!visitedTubes.contains(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else if (collectedStorage.add(neighborPos)) {
                    // Non-tube neighbor seen for the first time: collect its IItemHandler
                    IItemHandler handler =
                            level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
                    if (handler != null) {
                        Priority priority = resolvePriority(level, neighborPos, tubeBE, dir.ordinal());
                        entries.add(new HandlerEntry(handler, priority));
                    }

                    // Track the connected NI for energy routing
                    if (foundNi == null
                            && neighborState.getBlock() instanceof NetworkInterfaceBlock
                            && level.getBlockEntity(neighborPos) instanceof NetworkInterfaceBlockEntity ni) {
                        foundNi = ni;
                    }

                    // If this neighbor is a NetworkConnector, continue the BFS through it
                    // into any adjacent tubes (any color) that we haven't visited yet.
                    if (neighborState.getBlock() instanceof NetworkConnector && visitedConnectors.add(neighborPos)) {
                        for (Direction connDir : Direction.values()) {
                            BlockPos beyondPos = neighborPos.relative(connDir);
                            if (!visitedTubes.contains(beyondPos)
                                    && level.getBlockState(beyondPos).getBlock() instanceof TubeBlock) {
                                queue.add(beyondPos);
                            }
                        }
                    }
                }
            }
        }

        // Sort highest-priority first (HIGHEST.ordinal() == 4, LOWEST == 0)
        entries.sort(Comparator.comparingInt(e -> -e.priority().ordinal()));

        List<IItemHandler> handlers = new ArrayList<>(entries.size());
        for (HandlerEntry e : entries) {
            handlers.add(e.handler());
        }
        return new NetworkItemHandler(handlers, foundNi);
    }

    /**
     * Determines the effective priority for the storage block at {@code neighborPos}.
     *
     * <p>Priority source rules (highest precedence first):
     * <ol>
     *   <li>Storage Interface attachment on the tube face overrides everything.
     *   <li>Storage block's own {@code getPriority()} method.
     *   <li>Default: {@link Priority#NORMAL} for unknown types (hoppers, other mods).
     * </ol>
     */
    private static Priority resolvePriority(
            ServerLevel level, BlockPos neighborPos, TubeBlockEntity tubeBE, int faceIndex) {

        if (tubeBE != null && tubeBE.hasAttachment(faceIndex)) {
            return tubeBE.getAttachmentPriority(faceIndex);
        }

        BlockEntity neighborBE = level.getBlockEntity(neighborPos);
        if (neighborBE instanceof FilingCabinetBlockEntity fc) return fc.getPriority();
        if (neighborBE instanceof JunkDrawerBlockEntity jd) return jd.getPriority();
        if (neighborBE instanceof BulkStorageContainerBlockEntity bs) return bs.getPriority();

        return Priority.NORMAL;
    }

    private record HandlerEntry(IItemHandler handler, Priority priority) {}
}
