package net.bobofraggins.tremendousstorage.storage.xpjuice;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Registers client-side rendering for the XP Juice fluid. */
public final class XpJuiceClientEvents {

    private XpJuiceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {}, Registration.XP_JUICE_TYPE.get());
    }
}
