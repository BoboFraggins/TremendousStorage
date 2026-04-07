package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Source and Flowing variants of the Positive Vibes fluid.
 *
 * <p>Both classes delegate all behaviour to {@link BaseFlowingFluid} via the shared
 * {@link BaseFlowingFluid.Properties} object assembled in {@link Registration}.
 */
public final class PositiveVibesFluid {

    private PositiveVibesFluid() {}

    /** The still (source) variant of the Positive Vibes fluid. */
    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(Registration.HEALING_SALVE_FLUID_PROPS);
        }
    }

    /** The flowing variant of the Positive Vibes fluid. */
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(Registration.HEALING_SALVE_FLUID_PROPS);
        }
    }
}
