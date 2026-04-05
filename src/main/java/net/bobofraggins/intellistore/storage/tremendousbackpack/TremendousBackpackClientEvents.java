package net.bobofraggins.intellistore.storage.tremendousbackpack;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Tremendous Backpack — keybind and screen registration. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TremendousBackpackClientEvents {

    private TremendousBackpackClientEvents() {}

    /** Keybind to open the Tremendous Backpack UI. Default: B. */
    public static KeyMapping OPEN_TREMENDOUS_BACKPACK;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_TREMENDOUS_BACKPACK = new KeyMapping(
                "key.intellistore.open_tremendous_backpack",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.intellistore");
        event.register(OPEN_TREMENDOUS_BACKPACK);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.TREMENDOUS_BACKPACK_MENU.get(), TremendousBackpackScreen::new);
    }
}
