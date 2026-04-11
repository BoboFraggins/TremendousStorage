package net.bobofraggins.tremendousstorage.external.mobgrindinutils;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Source and Flowing variants of the fallback XP Juice fluid (used when Mob Grinding Utils is absent). */
public final class XpFluid {

    private XpFluid() {}

    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(MobGrindingUtilsIntegration.XP_FLUID_PROPS);
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(MobGrindingUtilsIntegration.XP_FLUID_PROPS);
        }
    }
}
