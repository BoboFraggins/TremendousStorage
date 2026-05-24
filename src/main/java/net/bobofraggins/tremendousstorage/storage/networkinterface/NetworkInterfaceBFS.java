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
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

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

    /**
     * @param noInsert when {@code true} the handler is included in the insert-order list (so the
     *     NI can read its contents and extract from it) but excluded from the priority-bucket map
     *     used for automatic insertion. Set for {@link AttachmentType#PASSIVE_INTERFACE} faces.
     */
    private record HandlerEntry(ResourceHandler<ItemResource> handler, Priority priority, boolean noInsert) {
        HandlerEntry(ResourceHandler<ItemResource> handler, Priority priority) {
            this(handler, priority, false);
        }
    }

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
        List<ResourceHandler<FluidResource>> tanks = new ArrayList<>();
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

        List<ResourceHandler<ItemResource>> insertOrder = new ArrayList<>(handlerEntries.size());
        for (HandlerEntry e : handlerEntries) insertOrder.add(e.handler());

        // Build priority buckets for two-phase insert (highest priority first via reverseOrder).
        // Passive-interface handlers are excluded so the NI never auto-inserts into them.
        TreeMap<Integer, List<ResourceHandler<ItemResource>>> buckets = new TreeMap<>(Comparator.reverseOrder());
        for (HandlerEntry e : handlerEntries) {
            if (!e.noInsert()) {
                buckets.computeIfAbsent(e.priority().ordinal(), k -> new ArrayList<>())
                        .add(e.handler());
            }
        }
        NavigableMap<Integer, List<ResourceHandler<ItemResource>>> insertBuckets =
                new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Integer, List<ResourceHandler<ItemResource>>> entry : buckets.entrySet()) {
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
            List<ResourceHandler<FluidResource>> tanks,
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
            List<ResourceHandler<FluidResource>> tanks,
            List<String> storageKeys) {

        // Fetch block entity once and reuse for both priority resolution and UI key lookup
        BlockEntity neighborBE = adjPos.equals(niPos) ? null : level.getBlockEntity(adjPos);

        // Never query the capability of a Network Interface — doing so would trigger its own
        // scan, causing infinite recursion. NIs are connectors/bridges only, not storage.
        if (neighborBE instanceof NetworkInterfaceBlockEntity) return;

        // Only expose this neighbor as network storage if the tube face has no attachment or
        // a StorageInterface. ImportInterface and ExportInterface own their neighbor for
        // pull/push operations exclusively and must not be used as generic insert targets.
        AttachmentType faceAttachment =
                tubeBE != null ? tubeBE.getAttachmentType(tubeDir.ordinal()) : AttachmentType.NONE;
        if (faceAttachment != AttachmentType.NONE && faceAttachment != AttachmentType.STORAGE_INTERFACE) {
            return;
        }

        // Resolve item/fluid capabilities
        ResourceHandler<ItemResource> itemCap =
                level.getCapability(Capabilities.Item.BLOCK, adjPos, tubeDir.getOpposite());
        ResourceHandler<FluidResource> fluidCap =
                level.getCapability(Capabilities.Fluid.BLOCK, adjPos, tubeDir.getOpposite());
        if (itemCap != null) {
            Priority priority = resolvePriority(tubeBE, tubeDir.ordinal(), neighborBE);
            // Blocks that expose both item AND fluid capabilities (e.g. Recycling Bin) are on
            // the network for their fluid only. Their item slot is marked no-insert so the NI
            // never automatically sends items there — it may be a void sink that destroys them.
            // Players can still route items in manually via a hopper or similar.
            boolean itemIsPassive = (fluidCap != null);
            handlerEntries.add(new HandlerEntry(itemCap, priority, itemIsPassive));
            // Also expose fluid so it is visible in the terminal (e.g. for weather control)
            if (fluidCap != null) {
                handlerEntries.add(new HandlerEntry(new TankItemAdapter(fluidCap), Priority.NORMAL));
                tanks.add(fluidCap);
            }
        } else if (fluidCap != null) {
            // Fluid-only block (e.g. Tank)
            handlerEntries.add(new HandlerEntry(new TankItemAdapter(fluidCap), Priority.NORMAL));
            tanks.add(fluidCap);
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
        if (neighborBE instanceof BarrelBlockEntity bb) return bb.getPriority();
        if (neighborBE instanceof net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity tank)
            return tank.getPriority();
        return Priority.NORMAL;
    }
}
