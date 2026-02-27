package net.bobofraggins.intellistore.healingsalve;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cauldron full of Healing Salve.
 *
 * <p>Created by right-clicking a water cauldron with a Glistering Melon Slice.
 * Supports two interactions (registered in {@link HealingSalveInteractions}):
 * <ul>
 *   <li>Empty bucket → Healing Salve Bucket (empties the cauldron)
 *   <li>Zombie Brain → spawns a Brain item entity (empties the cauldron)
 * </ul>
 */
public class HealingSalveCauldronBlock extends AbstractCauldronBlock {

    public static final MapCodec<HealingSalveCauldronBlock> CODEC =
            MapCodec.unit(Registration.HEALING_SALVE_CAULDRON::get);

    public HealingSalveCauldronBlock(Properties props) {
        super(props, HealingSalveInteractions.CAULDRON_INTERACTIONS);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return true; // always full — there is no partial level
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.9375;
    }
}
