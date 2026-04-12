package net.bobofraggins.tremendousstorage.storage.filingcabinet;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityScreen;
import net.bobofraggins.tremendousstorage.shared.ui.TankSettingsScreen;
import net.bobofraggins.tremendousstorage.storage.chest.ChestScreen;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ExportInterfaceScreen;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ImportInterfaceScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only event subscriber for screen registration. */
public final class FilingCabinetClientEvents {

    private FilingCabinetClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.FILING_CABINET_BE_TYPE.get(), FilingCabinetRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/filing_cabinet_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/filing_cabinet_drawer")));
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.FILING_CABINET_MENU.get(), FilingCabinetScreen::new);
        event.register(Registration.PRIORITY_MENU.get(), PriorityScreen::new);
        event.register(Registration.TREMENDOUS_CHEST_MENU.get(), ChestScreen::new);
        event.register(Registration.TANK_SETTINGS_MENU.get(), TankSettingsScreen::new);
        event.register(Registration.IMPORT_INTERFACE_MENU.get(), ImportInterfaceScreen::new);
        event.register(Registration.EXPORT_INTERFACE_MENU.get(), ExportInterfaceScreen::new);
    }
}
