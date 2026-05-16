package net.bobofraggins.tremendousstorage.storage.items;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * The world-placed fluid block for Positive Vibes.
 *
 * <p>Extends {@link LiquidBlock} which handles all fluid-level rendering and physics.
 * Properties must include {@code noLootTable()}, {@code noCollision()}, {@code liquid()},
 * {@code replaceable()}, and {@code pushReaction(DESTROY)}.
 */
public class PositiveVibesBlock extends LiquidBlock {

    public PositiveVibesBlock(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }
}
