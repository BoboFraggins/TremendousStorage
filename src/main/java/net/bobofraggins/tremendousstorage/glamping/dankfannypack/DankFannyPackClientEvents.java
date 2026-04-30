package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Dank Fanny Pack. */
public final class DankFannyPackClientEvents {

    private DankFannyPackClientEvents() {}

    /** Keybind to open the Dank Fanny Pack UI. Default: Alt-D. */
    public static KeyMapping OPEN_DANK_FANNY_PACK;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_DANK_FANNY_PACK = new KeyMapping(
                "key.tremendousstorage.open_dank_fanny_pack",
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_D,
                "key.categories.tremendousstorage");
        event.register(OPEN_DANK_FANNY_PACK);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.DANK_FANNY_PACK_MENU.get(), DankFannyPackScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(DankFannyPackClientTickHandler.class);

        if (!ModList.get().isLoaded("curios")) return;
        try {
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                    Registration.DANK_FANNY_PACK.get(), DankFannyPackCurioRenderer::new);
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
