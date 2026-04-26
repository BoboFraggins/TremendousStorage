package net.bobofraggins.tremendousstorage.storage.networkinterface;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-only event subscriber for Network Interface rendering and screen registration. */
public final class NetworkInterfaceClientEvents {

    private NetworkInterfaceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.NETWORK_INTERFACE_BE_TYPE.get(), NetworkInterfaceRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return NetworkInterfaceItemRenderer.getInstance();
            }
        };
        event.registerItem(extensions, Registration.NETWORK_INTERFACE_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.NETWORK_INTERFACE_MENU.get(), NetworkInterfaceScreen::new);
        event.register(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), AccessTerminalScreen::new);
    }
}
