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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * BFS utility that searches outward from a Storage Access Terminal to find the
 * nearest connected Network Interface lower half.
 *
 * <p>Scans all tube colors reachable from the SAT's adjacent faces. {@link NetworkConnector}
 * blocks (Filing Cabinet, Junk Drawer, Bulk Storage Container, SAT, Wireless Hub, Stirling
 * Engine) act as color-agnostic bridges: when the BFS encounters one, it continues through
 * all of its adjacent tubes of any color. Returns the {@link BlockPos} of the first NI lower
 * half found, or {@code null} if no NI is reachable.
 */
public final class AccessTerminalBFS {

    private AccessTerminalBFS() {}

    /**
     * Scans the tube network reachable from {@code satPos} and returns the block
     * position of the nearest Network Interface lower half, or {@code null} if none.
     */
    @Nullable
    public static BlockPos findNI(ServerLevel level, BlockPos satPos) {
        Set<BlockPos> visitedTubes = new HashSet<>();
        Set<BlockPos> visitedConnectors = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        // Seed the queue with every adjacent tube block (any color) and connector block
        for (Direction dir : Direction.values()) {
            BlockPos nb = satPos.relative(dir);
            BlockState st = level.getBlockState(nb);
            if (st.getBlock() instanceof TubeBlock) {
                queue.add(nb);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visitedTubes.add(pos)) continue;

            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof TubeBlock tube)) continue;

            for (Direction dir : Direction.values()) {
                if (!st.getValue(TubeBlock.DIR_PROPS[dir.ordinal()])) continue;

                BlockPos adj = pos.relative(dir);
                BlockState adjSt = level.getBlockState(adj);

                if (adjSt.getBlock() instanceof TubeBlock adjTube && adjTube.getColor() == tube.getColor()) {
                    // Continue BFS through same-color tubes
                    if (!visitedTubes.contains(adj)) {
                        queue.add(adj);
                    }
                } else if (adjSt.getBlock() instanceof NetworkInterfaceBlock
                        && adjSt.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    // Found a Network Interface lower half
                    return adj;
                } else if (adjSt.getBlock() instanceof NetworkConnector
                        && visitedConnectors.add(adj)) {
                    // Bridge through this connector into its adjacent tubes (any color)
                    for (Direction connDir : Direction.values()) {
                        BlockPos beyond = adj.relative(connDir);
                        if (!visitedTubes.contains(beyond)
                                && level.getBlockState(beyond).getBlock() instanceof TubeBlock) {
                            queue.add(beyond);
                        }
                    }
                }
            }
        }

        return null; // no NI reachable
    }
}
