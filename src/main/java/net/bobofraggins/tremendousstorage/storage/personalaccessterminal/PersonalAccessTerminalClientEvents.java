package net.bobofraggins.tremendousstorage.storage.personalaccessterminal;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubRenderer;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Personal Access Terminal — keybind, renderer, and screen registration. */
public final class PersonalAccessTerminalClientEvents {

    private PersonalAccessTerminalClientEvents() {}

    /** Keybind to open the Personal Access Terminal UI. Default: Ctrl+E. */
    public static KeyMapping OPEN_WIRELESS_SAT;

    @SubscribeEvent
    public static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tremendousstorage", "storage_tier"),
                net.bobofraggins.tremendousstorage.shared.storage.StorageTierTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.WIRELESS_HUB_BE_TYPE.get(), WirelessHubRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/wireless_hub_base"));
        event.register(ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/wireless_hub_dish"));
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_WIRELESS_SAT = new KeyMapping(
                "key.tremendousstorage.open_personal_access_terminal",
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_E,
                "key.categories.tremendousstorage");
        event.register(OPEN_WIRELESS_SAT);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.WIRELESS_HUB_MENU.get(), WirelessHubScreen::new);
    }
}
