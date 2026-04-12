package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side event subscriber that registers rendering properties for the Positive Vibes fluid. */
public final class PositiveVibesClientEvents {

    private PositiveVibesClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(
                            TremendousStorage.MODID, "fluid/positive_vibes_still");
                    private static final ResourceLocation FLOWING =
                            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_flow");

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING;
                    }
                },
                Registration.HEALING_SALVE_TYPE.get());
    }
}
