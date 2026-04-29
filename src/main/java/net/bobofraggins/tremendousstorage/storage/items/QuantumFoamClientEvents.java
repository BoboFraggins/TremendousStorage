package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side event subscriber that registers rendering properties for the Quantum Foam fluid. */
public final class QuantumFoamClientEvents {

    private QuantumFoamClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL =
                            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/quantum_foam_still");
                    private static final ResourceLocation FLOWING =
                            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/quantum_foam_flow");

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING;
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return STILL;
                    }
                },
                Registration.QUANTUM_FOAM_TYPE.get());
    }
}
