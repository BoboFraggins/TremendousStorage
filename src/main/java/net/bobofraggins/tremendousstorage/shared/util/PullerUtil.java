package net.bobofraggins.tremendousstorage.shared.util;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Shared utilities for blocks that have a puller upgrade. */
public final class PullerUtil {

    private PullerUtil() {}

    /**
     * Iterates over the enabled bits in {@code pullerSides}, resolves the adjacent item handler
     * capability for each, and invokes {@code pullCallback} when present.
     */
    public static void tickPuller(
            Level level,
            BlockPos pos,
            BlockState state,
            int pullerSides,
            Consumer<ResourceHandler<ItemResource>> pullCallback) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        for (int bit = 0; bit < 6; bit++) {
            if ((pullerSides & (1 << bit)) == 0) continue;
            Direction worldDir = bitToWorldDir(bit, facing);
            BlockPos adjacentPos = pos.relative(worldDir);
            ResourceHandler<ItemResource> cap =
                    level.getCapability(Capabilities.Item.BLOCK, adjacentPos, worldDir.getOpposite());
            if (cap == null) continue;
            pullCallback.accept(cap);
        }
    }

    /** Maps a puller-side bit index to a world {@link Direction} relative to {@code facing}. */
    public static Direction bitToWorldDir(int bit, Direction facing) {
        return switch (bit) {
            case 0 -> Direction.UP;
            case 1 -> Direction.DOWN;
            case 2 -> facing.getCounterClockWise();
            case 3 -> facing.getClockWise();
            case 4 -> facing;
            default -> facing.getOpposite();
        };
    }
}
