package net.bobofraggins.intellistore.storage.personalfilingcabinet;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetScreen;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Personal Filing Cabinet — keybind and screen registration. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PersonalFilingCabinetClientEvents {

    private PersonalFilingCabinetClientEvents() {}

    /** Keybind to open the PFC UI. Default: Ctrl+P. */
    public static KeyMapping OPEN_PERSONAL_FILING_CABINET;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_PERSONAL_FILING_CABINET = new KeyMapping(
                "key.intellistore.open_personal_filing_cabinet",
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.categories.intellistore");
        event.register(OPEN_PERSONAL_FILING_CABINET);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.PERSONAL_FILING_CABINET_MENU.get(), PersonalFilingCabinetScreen::new);
    }
}
