package net.bobofraggins.intellistore.shared.healingsalve;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Source and Flowing variants of the Healing Salve fluid.
 *
 * <p>Both classes delegate all behaviour to {@link BaseFlowingFluid} via the shared
 * {@link BaseFlowingFluid.Properties} object assembled in {@link Registration}.
 */
public final class HealingSalveFluid {

    private HealingSalveFluid() {}

    /** The still (source) variant of the Healing Salve fluid. */
    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(Registration.HEALING_SALVE_FLUID_PROPS);
        }
    }

    /** The flowing variant of the Healing Salve fluid. */
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(Registration.HEALING_SALVE_FLUID_PROPS);
        }
    }
}
