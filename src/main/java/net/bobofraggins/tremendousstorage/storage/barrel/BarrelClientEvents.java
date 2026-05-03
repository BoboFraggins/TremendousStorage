package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Registers the GUI screen for the Barrel. */
public final class BarrelClientEvents {

    private BarrelClientEvents() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.BARREL_MENU.get(), BarrelScreen::new);
    }
}
