package net.bobofraggins.tremendousstorage.storage.networkinterface;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlock;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankItemAdapter;
import net.bobofraggins.tremendousstorage.storage.tube.NetworkConnector;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlock;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.AttachmentType;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * BFS utility that scans a Network Interface's entire connected tube network.
 *
 * <p>{@link NetworkConnector} blocks (Filing Cabinet, Tremendous Chest, SAT, Wireless Hub)
 * act as bridges: when encountered, the BFS continues through their adjacent tubes.
 *
 * <p>Results are deduped by position, and returned as a {@link NetworkScanResult} with handlers
 * sorted for both insertion (highest-priority first) and extraction (lowest-priority first).
 *
 * <p>The network is considered valid when this NI is the <em>only</em> Network Interface
 * reachable on the combined tube network.
 */
public final class NetworkInterfaceBFS {

    private NetworkInterfaceBFS() {}

    private record HandlerEntry(IItemHandler handler, Priority priority) {}

    /** FE/t cost per attached SAT (flat; SATs have no tier upgrades). */
    private static final int SAT_COST = 5;
    /** FE/t cost per tube attachment (any type). */
    private static final int ATTACHMENT_COST = 1;

    /**
     * Scans the full network reachable from the Network Interface at {@code niPos}.
     *
     * @param level  server-side level
     * @param niPos  position of the Network Interface lower-half block entity
     * @return scan result (never null)
     */
    public static NetworkScanResult scan(ServerLevel level, BlockPos niPos) {

        Set<BlockPos> visitedTubes = new HashSet<>();
        Set<BlockPos> collectedStorage = new HashSet<>();
        // Connector blocks whose outgoing tube faces have already been enqueued
        Set<BlockPos> visitedConnectors = new HashSet<>();
        List<HandlerEntry> handlerEntries = new ArrayList<>();
        List<TankBlockEntity> tanks = new ArrayList<>();
        int tubeCount = 0;
        List<String> storageKeys = new ArrayList<>(); // ordered by discovery
        int otherNiCount = 0;
        // Power accounting — NI base cost scales with its tier
        BlockEntity niBE = level.getBlockEntity(niPos);
        int fePerTick = niBE instanceof NetworkInterfaceBlockEntity niBlockEntity
                ? niBlockEntity.getBaseFePerTick()
                : NetworkInterfaceBlockEntity.NI_COST;

        // Add the NI itself to the device list (processNeighbor skips NIs to prevent recursion)
        String niKey = blockListKey(niBE);
        if (niKey != null) storageKeys.add(niKey);

        // Single shared queue for the whole scan
        Deque<BlockPos> queue = new ArrayDeque<>();

        // Seed with every tube adjacent to the NI, and also handle
        // NetworkConnector blocks that are directly adjacent to the NI (no tube required).
        for (Direction niDir : Direction.values()) {
            BlockPos neighborPos = niPos.relative(niDir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof TubeBlock) {
                queue.add(neighborPos);
            } else if (neighborState.getBlock() instanceof NetworkConnector && collectedStorage.add(neighborPos)) {
                processNeighbor(
                        level, neighborPos, neighborState, niPos, niDir, null, handlerEntries, tanks, storageKeys);
                // Count power cost for SAT/Wireless Hub directly adjacent to NI
                if (neighborState.getBlock() instanceof AccessTerminalBlock) {
                    fePerTick += SAT_COST;
                } else {
                    BlockEntity adjBE = level.getBlockEntity(neighborPos);
                    if (adjBE instanceof WirelessHubBlockEntity hub) {
                        fePerTick += hub.getFePerTick();
                    }
                }
                // Bridge through the connector cluster to adjacent tubes of any color
                if (visitedConnectors.add(neighborPos)) {
                    fePerTick += bridgeConnectorCluster(
                            level,
                            neighborPos,
                            niPos,
                            visitedTubes,
                            visitedConnectors,
                            collectedStorage,
                            handlerEntries,
                            tanks,
                            storageKeys,
                            queue);
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visitedTubes.add(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TubeBlock)) continue;

            tubeCount++;

            TubeBlockEntity tubeBE = level.getBlockEntity(pos) instanceof TubeBlockEntity tbe ? tbe : null;

            // Count tube attachment power cost
            if (tubeBE != null) {
                for (int i = 0; i < 6; i++) {
                    if (tubeBE.getAttachmentType(i) != AttachmentType.NONE) {
                        fePerTick += ATTACHMENT_COST;
                    }
                }
            }

            for (Direction dir : Direction.values()) {
                if (!state.getValue(TubeBlock.DIR_PROPS[dir.ordinal()])) continue;

                BlockPos adjPos = pos.relative(dir);
                BlockState adjState = level.getBlockState(adjPos);

                if (adjState.getBlock() instanceof TubeBlock) {
                    if (!visitedTubes.contains(adjPos)) {
                        queue.add(adjPos);
                    }
                } else if (collectedStorage.add(adjPos)) {
                    // First time we see this non-tube block
                    processNeighbor(level, adjPos, adjState, niPos, dir, tubeBE, handlerEntries, tanks, storageKeys);

                    // Check if it's another NI
                    if (adjState.getBlock() instanceof NetworkInterfaceBlock && !adjPos.equals(niPos)) {
                        otherNiCount++;
                    }

                    // Count SAT and Wireless Hub power cost
                    if (adjState.getBlock() instanceof AccessTerminalBlock) {
                        fePerTick += SAT_COST;
                    } else {
                        BlockEntity adjBE = adjPos.equals(niPos) ? null : level.getBlockEntity(adjPos);
                        if (adjBE instanceof WirelessHubBlockEntity hub) {
                            fePerTick += hub.getFePerTick();
                        }
                    }

                    // If this neighbor is a NetworkConnector, bridge through it into
                    // adjacent tubes and connectors of any color
                    if (adjState.getBlock() instanceof NetworkConnector && visitedConnectors.add(adjPos)) {
                        fePerTick += bridgeConnectorCluster(
                                level,
                                adjPos,
                                niPos,
                                visitedTubes,
                                visitedConnectors,
                                collectedStorage,
                                handlerEntries,
                                tanks,
                                storageKeys,
                                queue);
                    }
                }
            }
        }

        // Sort handlers by priority
        handlerEntries.sort(Comparator.comparingInt(e -> -e.priority().ordinal())); // highest first

        List<IItemHandler> insertOrder = new ArrayList<>(handlerEntries.size());
        for (HandlerEntry e : handlerEntries) insertOrder.add(e.handler());

        // Build priority buckets for two-phase insert (highest priority first via reverseOrder)
        TreeMap<Integer, List<IItemHandler>> buckets = new TreeMap<>(Comparator.reverseOrder());
        for (HandlerEntry e : handlerEntries) {
            buckets.computeIfAbsent(e.priority().ordinal(), k -> new ArrayList<>())
                    .add(e.handler());
        }
        NavigableMap<Integer, List<IItemHandler>> insertBuckets = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Integer, List<IItemHandler>> entry : buckets.entrySet()) {
            insertBuckets.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        // Build UI block list: storage blocks sorted alphabetically, then tubes
        List<AttachedEntry> blockList = new ArrayList<>();
        Map<String, Integer> storageCounts = new HashMap<>();
        for (String key : storageKeys) storageCounts.merge(key, 1, Integer::sum);
        List<String> sortedKeys = new ArrayList<>(storageCounts.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            blockList.add(new AttachedEntry(key, storageCounts.get(key)));
        }
        if (tubeCount > 0) {
            blockList.add(new AttachedEntry("block.tremendousstorage.tube", tubeCount));
        }

        return new NetworkScanResult(
                List.copyOf(insertOrder),
                Collections.unmodifiableNavigableMap(insertBuckets),
                List.copyOf(blockList),
                otherNiCount == 0,
                fePerTick,
                Set.copyOf(visitedTubes),
                List.copyOf(tanks));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Flood-fills through all directly-adjacent {@link NetworkConnector} blocks reachable from
     * {@code startConnector} without going through tubes. Enqueues any adjacent {@link TubeBlock}s
     * into {@code tubeQueue} and processes each newly-discovered connector via
     * {@link #processNeighbor}. Returns the additional FE/t cost for the discovered connectors.
     *
     * <p>{@code startConnector} must already be in {@code visitedConnectors} before calling.
     */
    private static int bridgeConnectorCluster(
            ServerLevel level,
            BlockPos startConnector,
            BlockPos niPos,
            Set<BlockPos> visitedTubes,
            Set<BlockPos> visitedConnectors,
            Set<BlockPos> collectedStorage,
            List<HandlerEntry> handlerEntries,
            List<TankBlockEntity> tanks,
            List<String> storageKeys,
            Deque<BlockPos> tubeQueue) {
        int feCost = 0;
        Deque<BlockPos> pending = new ArrayDeque<>();
        pending.add(startConnector);
        while (!pending.isEmpty()) {
            BlockPos connPos = pending.poll();
            for (Direction dir : Direction.values()) {
                BlockPos adj = connPos.relative(dir);
                BlockState adjState = level.getBlockState(adj);
                if (!visitedTubes.contains(adj) && adjState.getBlock() instanceof TubeBlock) {
                    tubeQueue.add(adj);
                } else if (adjState.getBlock() instanceof NetworkConnector && collectedStorage.add(adj)) {
                    processNeighbor(level, adj, adjState, niPos, dir, null, handlerEntries, tanks, storageKeys);
                    if (adjState.getBlock() instanceof AccessTerminalBlock) {
                        feCost += SAT_COST;
                    } else {
                        BlockEntity adjBE = level.getBlockEntity(adj);
                        if (adjBE instanceof WirelessHubBlockEntity hub) feCost += hub.getFePerTick();
                    }
                    if (visitedConnectors.add(adj)) {
                        pending.add(adj);
                    }
                }
            }
        }
        return feCost;
    }

    private static void processNeighbor(
            ServerLevel level,
            BlockPos adjPos,
            BlockState adjState,
            BlockPos niPos,
            Direction tubeDir,
            TubeBlockEntity tubeBE,
            List<HandlerEntry> handlerEntries,
            List<TankBlockEntity> tanks,
            List<String> storageKeys) {

        // Fetch block entity once and reuse for both priority resolution and UI key lookup
        BlockEntity neighborBE = adjPos.equals(niPos) ? null : level.getBlockEntity(adjPos);

        // Never query the capability of a Network Interface — doing so would trigger its own
        // scan, causing infinite recursion. NIs are connectors/bridges only, not storage.
        if (neighborBE instanceof NetworkInterfaceBlockEntity) return;

        // Resolve IItemHandler capability
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, adjPos, tubeDir.getOpposite());
        if (handler != null) {
            Priority priority = resolvePriority(tubeBE, tubeDir.ordinal(), neighborBE);
            handlerEntries.add(new HandlerEntry(handler, priority));
        } else if (neighborBE instanceof TankBlockEntity tank) {
            handlerEntries.add(new HandlerEntry(new TankItemAdapter(tank), Priority.NORMAL));
            tanks.add(tank);
        }

        // Record block type for UI list
        String key = blockListKey(neighborBE);
        if (key != null) storageKeys.add(key);
    }

    /** Returns the display name for the UI list, or {@code null} if the block should be hidden. */
    private static String blockListKey(BlockEntity be) {
        if (be instanceof NetworkListable nl) return nl.getNetworkName();
        if (be instanceof MenuProvider mp) return mp.getDisplayName().getString();
        return null;
    }

    private static Priority resolvePriority(TubeBlockEntity tubeBE, int faceIndex, BlockEntity neighborBE) {

        if (tubeBE != null && tubeBE.hasAttachment(faceIndex)) {
            return tubeBE.getAttachmentPriority(faceIndex);
        }

        if (neighborBE instanceof FilingCabinetBlockEntity fc) return fc.getPriority();
        if (neighborBE instanceof ChestBlockEntity bs) return bs.getPriority();
        return Priority.NORMAL;
    }
}
