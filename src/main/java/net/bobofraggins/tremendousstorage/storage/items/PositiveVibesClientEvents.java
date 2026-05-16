package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.Identifier;
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
                    private static final Identifier STILL =
                            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_still");
                    private static final Identifier FLOWING =
                            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_flow");

                    @Override
                    public Identifier getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public Identifier getFlowingTexture() {
                        return FLOWING;
                    }

                    @Override
                    public Identifier getOverlayTexture() {
                        return STILL;
                    }
                },
                Registration.POSITIVE_VIBES_TYPE.get());
    }
}
