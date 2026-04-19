package net.bobofraggins.tremendousstorage.storage.honey;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/** The world-placed fluid block for Honey. */
public class HoneyBlock extends LiquidBlock {

    public HoneyBlock(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }
}
