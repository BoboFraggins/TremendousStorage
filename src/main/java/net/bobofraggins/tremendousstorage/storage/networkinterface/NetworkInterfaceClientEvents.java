package net.bobofraggins.tremendousstorage.storage.networkinterface;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

/** Client-only event subscriber for Network Interface rendering and screen registration. */
public final class NetworkInterfaceClientEvents {

    private NetworkInterfaceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.NETWORK_INTERFACE_BE_TYPE.get(), NetworkInterfaceRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath("tremendousstorage", "network_interface_renderer"),
                NetworkInterfaceItemRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.NETWORK_INTERFACE_MENU.get(), NetworkInterfaceScreen::new);
        event.register(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), AccessTerminalScreen::new);
    }
}
