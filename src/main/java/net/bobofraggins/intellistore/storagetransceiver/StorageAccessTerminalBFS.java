package net.bobofraggins.intellistore.storagetransceiver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.networkinterface.NetworkInterfaceBlock;
import net.bobofraggins.intellistore.tube.TubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * BFS utility that searches outward from a Storage Access Terminal to find the
 * nearest connected Network Interface lower half.
 *
 * <p>Scans all tube colors reachable from the SAT's adjacent faces, following
 * same-color tube connections. Returns the {@link BlockPos} of the first NI
 * lower half found, or {@code null} if no NI is reachable.
 */
public final class StorageAccessTerminalBFS {

    private StorageAccessTerminalBFS() {}

    /**
     * Scans the tube network reachable from {@code satPos} and returns the block
     * position of the nearest Network Interface lower half, or {@code null} if none.
     */
    @Nullable
    public static BlockPos findNI(ServerLevel level, BlockPos satPos) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        // Seed the queue with every adjacent tube block (any color)
        for (Direction dir : Direction.values()) {
            BlockPos nb = satPos.relative(dir);
            BlockState st = level.getBlockState(nb);
            if (st.getBlock() instanceof TubeBlock) {
                queue.add(nb);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;

            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof TubeBlock tube)) continue;

            DyeColor color = tube.getColor();

            for (Direction dir : Direction.values()) {
                if (!st.getValue(TubeBlock.DIR_PROPS[dir.ordinal()])) continue;

                BlockPos adj = pos.relative(dir);
                BlockState adjSt = level.getBlockState(adj);

                if (adjSt.getBlock() instanceof TubeBlock adjTube && adjTube.getColor() == color) {
                    // Continue BFS through same-color tubes
                    if (!visited.contains(adj)) {
                        queue.add(adj);
                    }
                } else if (adjSt.getBlock() instanceof NetworkInterfaceBlock
                        && adjSt.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    // Found a Network Interface lower half
                    return adj;
                }
            }
        }

        return null; // no NI reachable
    }
}
