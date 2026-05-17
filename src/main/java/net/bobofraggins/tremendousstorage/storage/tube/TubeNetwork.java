package net.bobofraggins.tremendousstorage.storage.tube;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Utility class that builds a {@link NetworkItemHandler} for a tube network.
 *
 * <p>A tube network is the connected component of all {@link TubeBlock} positions reachable
 * from a starting position. {@link NetworkConnector} blocks (Filing Cabinet, Tremendous Chest,
 * SAT, Wireless Hub) act as bridges: their {@code IItemHandler} is collected as a storage
 * endpoint and the BFS continues through all of their adjacent tubes.
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
     * @return composite handler for the entire network; never null
     */
    public static NetworkItemHandler buildNetworkView(ServerLevel level, BlockPos start) {

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
            if (!(state.getBlock() instanceof TubeBlock)) continue;

            TubeBlockEntity tubeBE = level.getBlockEntity(pos) instanceof TubeBlockEntity tbe ? tbe : null;

            for (Direction dir : Direction.values()) {
                // Only follow active connections (tube arm extends toward this face)
                if (!state.getValue(TubeBlock.DIR_PROPS[dir.ordinal()])) continue;

                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof TubeBlock) {
                    if (!visitedTubes.contains(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else if (collectedStorage.add(neighborPos)) {
                    // Non-tube neighbor seen for the first time.
                    // Only include it as network storage when the face has a Storage Interface
                    // (or no attachment). Import/Export interfaces own their neighbor exclusively
                    // for pull/push operations and must not expose it as a network storage target.
                    AttachmentType faceAttachment =
                            tubeBE != null ? tubeBE.getAttachmentType(dir.ordinal()) : AttachmentType.NONE;
                    if (faceAttachment == AttachmentType.NONE || faceAttachment == AttachmentType.STORAGE_INTERFACE) {
                        ResourceHandler<ItemResource> cap =
                                level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite());
                        if (cap != null) {
                            Priority priority = resolvePriority(level, neighborPos, tubeBE, dir.ordinal());
                            entries.add(new HandlerEntry(cap, priority));
                        }
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

        List<ResourceHandler<ItemResource>> handlers = new ArrayList<>(entries.size());
        for (HandlerEntry e : entries) handlers.add(e.handler());
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
        if (neighborBE instanceof ChestBlockEntity bs) return bs.getPriority();

        return Priority.NORMAL;
    }

    private record HandlerEntry(ResourceHandler<ItemResource> handler, Priority priority) {}
}
