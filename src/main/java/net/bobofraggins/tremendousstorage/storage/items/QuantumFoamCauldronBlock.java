package net.bobofraggins.tremendousstorage.storage.items;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cauldron full of Quantum Foam.
 *
 * <p>Supports two interactions (registered in {@link QuantumFoamInteractions}):
 * <ul>
 *   <li>Empty bucket → Quantum Foam Bucket (empties the cauldron)
 *   <li>Glistering Melon Slice → Positive Vibes Cauldron (transforms the fluid)
 * </ul>
 */
public class QuantumFoamCauldronBlock extends AbstractCauldronBlock {

    public static final MapCodec<QuantumFoamCauldronBlock> CODEC =
            MapCodec.unit(Registration.QUANTUM_FOAM_CAULDRON::get);

    public QuantumFoamCauldronBlock(Properties props) {
        super(props, QuantumFoamInteractions.CAULDRON_INTERACTIONS);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.9375;
    }
}
