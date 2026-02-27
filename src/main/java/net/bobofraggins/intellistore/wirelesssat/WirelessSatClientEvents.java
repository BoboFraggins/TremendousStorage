package net.bobofraggins.intellistore.wirelesssat;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Wireless SAT — keybind, renderer, and screen registration. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WirelessSatClientEvents {

    private WirelessSatClientEvents() {}

    /**
     * Keybind to open the Wireless SAT UI when it is in a Curios slot (or anywhere in inventory).
     * Default is unbound — players can set it to any preferred key in Controls.
     */
    public static KeyMapping OPEN_WIRELESS_SAT;

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.WIRELESS_HUB_BE_TYPE.get(), WirelessHubRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_WIRELESS_SAT = new KeyMapping(
                "key.intellistore.open_wireless_sat",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default
                "key.categories.intellistore");
        event.register(OPEN_WIRELESS_SAT);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.WIRELESS_HUB_MENU.get(), WirelessHubScreen::new);
    }
}
