package net.bobofraggins.tremendousstorage.storage.items;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cauldron full of Positive Vibes.
 *
 * <p>Created by right-clicking a water cauldron with a Glistering Melon Slice.
 * Supports two interactions (registered in {@link PositiveVibesInteractions}):
 * <ul>
 *   <li>Empty bucket → Positive Vibes Bucket (empties the cauldron)
 *   <li>Zombie Brain → spawns a Brain item entity (empties the cauldron)
 * </ul>
 */
public class PositiveVibesCauldronBlock extends AbstractCauldronBlock {

    public static final MapCodec<PositiveVibesCauldronBlock> CODEC =
            MapCodec.unit(Registration.HEALING_SALVE_CAULDRON::get);

    public PositiveVibesCauldronBlock(Properties props) {
        super(props, PositiveVibesInteractions.CAULDRON_INTERACTIONS);
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
