package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side event subscriber that registers rendering properties for the Positive Vibes fluid. */
public final class PositiveVibesClientEvents {

    private PositiveVibesClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {}, Registration.POSITIVE_VIBES_TYPE.get());
    }
}
