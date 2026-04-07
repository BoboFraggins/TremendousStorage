package net.bobofraggins.tremendousstorage.external.productivemetalworks;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Source and Flowing variants of the Molten Lazurite fluid. */
public final class MoltenLazuriteFluid {

    private MoltenLazuriteFluid() {}

    public static class Source extends BaseFlowingFluid.Source {
        public Source() {
            super(ProductiveMetalworksIntegration.MOLTEN_LAZURITE_FLUID_PROPS);
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing() {
            super(ProductiveMetalworksIntegration.MOLTEN_LAZURITE_FLUID_PROPS);
        }
    }
}
