package net.bobofraggins.tremendousstorage.storage.tube;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.StorageInterfaceScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only event subscriber for Tube rendering and screen registration. */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TubeClientEvents {

    private TubeClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TUBE_BE_TYPE.get(), TubeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.STORAGE_INTERFACE_MENU.get(), StorageInterfaceScreen::new);
    }
}
