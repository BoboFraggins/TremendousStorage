package net.bobofraggins.tremendousstorage.storage.networkinterface;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only event subscriber for Network Interface rendering and screen registration. */
public final class NetworkInterfaceClientEvents {

    private NetworkInterfaceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.NETWORK_INTERFACE_BE_TYPE.get(), NetworkInterfaceRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/brain_3d")));
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.NETWORK_INTERFACE_MENU.get(), NetworkInterfaceScreen::new);
        event.register(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), AccessTerminalScreen::new);
    }
}
