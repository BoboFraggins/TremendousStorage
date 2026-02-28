package net.bobofraggins.intellistore.storage.networkinterface;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceScreen;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only event subscriber for Network Interface rendering and screen registration. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NetworkInterfaceClientEvents {

    private NetworkInterfaceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.NETWORK_INTERFACE_BE_TYPE.get(), NetworkInterfaceRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.NETWORK_INTERFACE_MENU.get(), NetworkInterfaceScreen::new);
        event.register(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), AccessTerminalScreen::new);
    }
}
