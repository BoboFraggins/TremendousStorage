package net.bobofraggins.intellistore.filingcabinet;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.BreakerInterfaceScreen;
import net.bobofraggins.intellistore.ui.PersonalFilingCabinetScreen;
import net.bobofraggins.intellistore.ui.ExportInterfaceScreen;
import net.bobofraggins.intellistore.ui.FilingCabinetScreen;
import net.bobofraggins.intellistore.ui.ImportInterfaceScreen;
import net.bobofraggins.intellistore.ui.PlacerInterfaceScreen;
import net.bobofraggins.intellistore.ui.PriorityScreen;
import net.bobofraggins.intellistore.ui.TankSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only event subscriber for screen registration. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FilingCabinetClientEvents {

    private FilingCabinetClientEvents() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.FILING_CABINET_MENU.get(), FilingCabinetScreen::new);
        event.register(Registration.PRIORITY_MENU.get(), PriorityScreen::new);
        event.register(Registration.TANK_SETTINGS_MENU.get(), TankSettingsScreen::new);
        event.register(Registration.IMPORT_INTERFACE_MENU.get(), ImportInterfaceScreen::new);
        event.register(Registration.EXPORT_INTERFACE_MENU.get(), ExportInterfaceScreen::new);
        event.register(Registration.PLACER_INTERFACE_MENU.get(), PlacerInterfaceScreen::new);
        event.register(Registration.BREAKER_INTERFACE_MENU.get(), BreakerInterfaceScreen::new);
        event.register(Registration.PERSONAL_FILING_CABINET_MENU.get(), PersonalFilingCabinetScreen::new);
    }
}
