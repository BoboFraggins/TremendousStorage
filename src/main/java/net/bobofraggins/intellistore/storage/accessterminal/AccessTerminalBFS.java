package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.intellistore.storage.tube.NetworkConnector;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BFS utility that searches outward from a network device to find the nearest connected
 * Network Interface.
 *
 * <p>Two node types are traversed:
 * <ul>
 *   <li>{@link TubeBlock} — directional: only follows faces where the tube has a connection.
 *       Adjacent tubes and {@link NetworkConnector} blocks reachable via a connected face
 *       are added to the queue.
 *   <li>{@link NetworkConnector} (Filing Cabinet, Bulk Storage, Access Terminal,
 *       Wireless Hub) — omnidirectional: checks all six faces freely,
 *       reaching adjacent tubes or other connectors without requiring tube attachments.
 *       Two connectors that are directly adjacent are therefore on the same network.
 * </ul>
 *
 * <p>Returns the {@link BlockPos} of the first {@link NetworkInterfaceBlock} reachable,
 * or {@code null} if none.
 */
public final class AccessTerminalBFS {

    private AccessTerminalBFS() {}

    /**
     * Scans the network reachable from {@code startPos} and returns the position of the
     * nearest Network Interface, or {@code null} if none is reachable.
     */
    @Nullable
    public static BlockPos findNI(ServerLevel level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        // Seed: directly adjacent NI, tubes, and connectors
        for (Direction dir : Direction.values()) {
            BlockPos nb = startPos.relative(dir);
            Block block = level.getBlockState(nb).getBlock();
            if (block instanceof NetworkInterfaceBlock) return nb;
            if (block instanceof TubeBlock || block instanceof NetworkConnector) {
                if (visited.add(nb)) queue.add(nb);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState st = level.getBlockState(pos);

            if (st.getBlock() instanceof TubeBlock) {
                // Tubes: only follow connected faces
                for (Direction dir : Direction.values()) {
                    if (!st.getValue(TubeBlock.DIR_PROPS[dir.ordinal()])) continue;
                    BlockPos adj = pos.relative(dir);
                    Block adjBlock = level.getBlockState(adj).getBlock();
                    if (adjBlock instanceof NetworkInterfaceBlock) return adj;
                    if (adjBlock instanceof TubeBlock) {
                        if (visited.add(adj)) queue.add(adj);
                    } else if (adjBlock instanceof NetworkConnector) {
                        if (visited.add(adj)) queue.add(adj);
                    }
                }
            } else if (st.getBlock() instanceof NetworkConnector) {
                // Connectors: check all six faces freely
                for (Direction dir : Direction.values()) {
                    BlockPos adj = pos.relative(dir);
                    Block adjBlock = level.getBlockState(adj).getBlock();
                    if (adjBlock instanceof NetworkInterfaceBlock) return adj;
                    if (adjBlock instanceof TubeBlock || adjBlock instanceof NetworkConnector) {
                        if (visited.add(adj)) queue.add(adj);
                    }
                }
            }
        }

        return null; // no NI reachable
    }
}
