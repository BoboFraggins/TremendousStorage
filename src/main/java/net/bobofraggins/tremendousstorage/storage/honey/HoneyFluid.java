package net.bobofraggins.tremendousstorage.storage.honey;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Source and Flowing variants of the Honey fluid. */
public final class HoneyFluid {

    private HoneyFluid() {}

    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(Registration.HONEY_FLUID_PROPS);
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(Registration.HONEY_FLUID_PROPS);
        }
    }
}
