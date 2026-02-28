package net.bobofraggins.intellistore.storage.networkinterface;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlockEntity;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBlock;
import net.bobofraggins.intellistore.storage.tubeattachments.AttachmentType;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.bobofraggins.intellistore.storage.tube.TubeBlockEntity;
import net.bobofraggins.intellistore.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * BFS utility that scans a Network Interface's entire connected tube network.
 *
 * <p>A Network Interface connects to <em>all</em> tube colors. For each directly-adjacent
 * {@link TubeBlock}, this scanner starts a same-color BFS and collects every storage block
 * reachable from those tubes. {@link NetworkConnector} blocks (Filing Cabinet, Junk Drawer,
 * Bulk Storage Container, SAT, Wireless Hub, Stirling Engine) act as color-agnostic bridges:
 * when encountered, the BFS continues through their adjacent tubes of any color, allowing
 * different-colored tube runs to form a single unified network.
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

    /** FE/t cost per attached SAT. */
    private static final int SAT_COST = 5;
    /** FE/t cost per Wireless Hub. */
    private static final int HUB_COST = 25;
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
        Map<String, Integer> tubeCounts = new HashMap<>(); // color name → count
        List<String> storageKeys = new ArrayList<>(); // ordered by discovery
        int otherNiCount = 0;
        // Power accounting
        int fePerTick = NetworkInterfaceBlockEntity.NI_COST; // base NI cost

        // Single shared queue for the whole scan; tubes of any color may be enqueued
        // (the NI itself is a color-agnostic entry point, and connectors bridge colors)
        Deque<BlockPos> queue = new ArrayDeque<>();

        // Seed with every tube adjacent to the NI (any color)
        for (Direction niDir : Direction.values()) {
            BlockPos neighborPos = niPos.relative(niDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof TubeBlock) {
                queue.add(neighborPos);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visitedTubes.add(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TubeBlock tb)) continue;

            // Count this tube for the UI list
            tubeCounts.merge(tb.getColor().getName(), 1, Integer::sum);

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

                if (adjState.getBlock() instanceof TubeBlock adjTube
                        && adjTube.getColor() == tb.getColor()) {
                    // Continue BFS through same-color tubes
                    if (!visitedTubes.contains(adjPos)) {
                        queue.add(adjPos);
                    }
                } else if (collectedStorage.add(adjPos)) {
                    // First time we see this non-tube block
                    processNeighbor(level, adjPos, adjState, niPos, dir, tubeBE, handlerEntries, storageKeys);

                    // Check if it's another NI lower half
                    if (adjState.getBlock() instanceof NetworkInterfaceBlock
                            && adjState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                            && !adjPos.equals(niPos)) {
                        otherNiCount++;
                    }

                    // Count SAT and Wireless Hub power cost
                    if (adjState.getBlock() instanceof AccessTerminalBlock) {
                        fePerTick += SAT_COST;
                    } else {
                        BlockEntity adjBE = adjPos.equals(niPos) ? null : level.getBlockEntity(adjPos);
                        if (adjBE instanceof WirelessHubBlockEntity) {
                            fePerTick += HUB_COST;
                        }
                    }

                    // If this neighbor is a NetworkConnector, bridge through it into
                    // adjacent tubes of any color
                    if (adjState.getBlock() instanceof NetworkConnector
                            && visitedConnectors.add(adjPos)) {
                        for (Direction connDir : Direction.values()) {
                            BlockPos beyondPos = adjPos.relative(connDir);
                            if (!visitedTubes.contains(beyondPos)
                                    && level.getBlockState(beyondPos).getBlock() instanceof TubeBlock) {
                                queue.add(beyondPos);
                            }
                        }
                    }
                }
            }
        }

        // Sort handlers by priority
        handlerEntries.sort(Comparator.comparingInt(e -> -e.priority().ordinal())); // highest first

        List<IItemHandler> insertOrder = new ArrayList<>(handlerEntries.size());
        for (HandlerEntry e : handlerEntries) insertOrder.add(e.handler());

        List<IItemHandler> extractOrder = new ArrayList<>(insertOrder);
        Collections.reverse(extractOrder); // lowest priority first

        // Build UI block list: storage blocks first, then tubes
        List<AttachedEntry> blockList = new ArrayList<>();
        // Aggregate duplicate storage keys
        Map<String, Integer> storageCounts = new HashMap<>();
        for (String key : storageKeys) storageCounts.merge(key, 1, Integer::sum);
        // Defined display order for storage types
        List<String> storageOrder = List.of(
                "block.intellistore.filing_cabinet",
                "block.intellistore.junk_drawer",
                "block.intellistore.bulk_storage_container",
                "block.intellistore.fluid_tank");
        for (String key : storageOrder) {
            int count = storageCounts.getOrDefault(key, 0);
            if (count > 0) blockList.add(new AttachedEntry(key, count));
        }
        // Tube entries (sorted by color name for determinism)
        List<String> sortedColors = new ArrayList<>(tubeCounts.keySet());
        Collections.sort(sortedColors);
        for (String colorName : sortedColors) {
            blockList.add(new AttachedEntry("block.intellistore." + colorName + "_tube", tubeCounts.get(colorName)));
        }

        return new NetworkScanResult(
                List.copyOf(insertOrder),
                List.copyOf(extractOrder),
                List.copyOf(blockList),
                otherNiCount == 0,
                fePerTick);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void processNeighbor(
            ServerLevel level,
            BlockPos adjPos,
            BlockState adjState,
            BlockPos niPos,
            Direction tubeDir,
            TubeBlockEntity tubeBE,
            List<HandlerEntry> handlerEntries,
            List<String> storageKeys) {

        // Fetch block entity once and reuse for both priority resolution and UI key lookup
        BlockEntity neighborBE = adjPos.equals(niPos) ? null : level.getBlockEntity(adjPos);

        // Resolve IItemHandler capability
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, adjPos, tubeDir.getOpposite());
        if (handler != null) {
            Priority priority = resolvePriority(tubeBE, tubeDir.ordinal(), neighborBE);
            handlerEntries.add(new HandlerEntry(handler, priority));
        }

        // Record block type for UI list
        String key = blockListKey(neighborBE);
        if (key != null) storageKeys.add(key);
    }

    /** Returns the translation key for the UI list, or {@code null} if the block should be hidden. */
    private static String blockListKey(BlockEntity be) {
        if (be instanceof FilingCabinetBlockEntity) return "block.intellistore.filing_cabinet";
        if (be instanceof JunkDrawerBlockEntity) return "block.intellistore.junk_drawer";
        if (be instanceof BulkStorageContainerBlockEntity) return "block.intellistore.bulk_storage_container";
        if (be instanceof FluidTankBlockEntity) return "block.intellistore.fluid_tank";
        return null;
    }

    private static Priority resolvePriority(TubeBlockEntity tubeBE, int faceIndex, BlockEntity neighborBE) {

        if (tubeBE != null && tubeBE.hasAttachment(faceIndex)) {
            return tubeBE.getAttachmentPriority(faceIndex);
        }

        if (neighborBE instanceof FilingCabinetBlockEntity fc) return fc.getPriority();
        if (neighborBE instanceof JunkDrawerBlockEntity jd) return jd.getPriority();
        if (neighborBE instanceof BulkStorageContainerBlockEntity bs) return bs.getPriority();
        return Priority.NORMAL;
    }
}
