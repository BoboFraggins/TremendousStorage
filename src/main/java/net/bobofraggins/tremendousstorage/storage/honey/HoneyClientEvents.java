package net.bobofraggins.tremendousstorage.storage.honey;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Registers client-side rendering for the Honey fluid. */
public final class HoneyClientEvents {

    private HoneyClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {}, Registration.HONEY_TYPE.get());
    }
}
