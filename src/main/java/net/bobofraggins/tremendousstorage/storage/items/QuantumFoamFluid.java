package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Source and Flowing variants of the Quantum Foam fluid.
 *
 * <p>Both classes delegate all behaviour to {@link BaseFlowingFluid} via the shared
 * {@link BaseFlowingFluid.Properties} object assembled in {@link Registration}.
 */
public final class QuantumFoamFluid {

    private QuantumFoamFluid() {}

    /** The still (source) variant of the Quantum Foam fluid. */
    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(Registration.QUANTUM_FOAM_FLUID_PROPS);
        }
    }

    /** The flowing variant of the Quantum Foam fluid. */
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(Registration.QUANTUM_FOAM_FLUID_PROPS);
        }
    }
}
