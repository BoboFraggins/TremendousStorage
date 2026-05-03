package net.bobofraggins.tremendousstorage.storage.xpjuice;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Source and Flowing variants of the XP Juice fluid. */
public final class XpJuiceFluid {

    private XpJuiceFluid() {}

    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(Registration.XP_JUICE_FLUID_PROPS);
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(Registration.XP_JUICE_FLUID_PROPS);
        }
    }
}
