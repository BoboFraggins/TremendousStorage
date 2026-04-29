package net.bobofraggins.tremendousstorage.storage.items;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * The world-placed fluid block for Quantum Foam.
 *
 * <p>Extends {@link LiquidBlock} which handles all fluid-level rendering and physics.
 * Properties must include {@code noLootTable()}, {@code noCollission()}, {@code liquid()},
 * {@code replaceable()}, and {@code pushReaction(DESTROY)}.
 */
public class QuantumFoamBlock extends LiquidBlock {

    public QuantumFoamBlock(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }
}
