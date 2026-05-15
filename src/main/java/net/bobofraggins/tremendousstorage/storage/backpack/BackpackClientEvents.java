package net.bobofraggins.tremendousstorage.storage.backpack;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Tremendous Backpack — keybind, screen, BESR, and worn layer. */
public final class BackpackClientEvents {

    private BackpackClientEvents() {}

    /** Keybind to open the Tremendous Backpack UI. Default: B. */
    public static KeyMapping OPEN_TREMENDOUS_BACKPACK;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_TREMENDOUS_BACKPACK = new KeyMapping(
                "key.tremendousstorage.open_backpack",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.tremendousstorage");
        event.register(OPEN_TREMENDOUS_BACKPACK);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.TREMENDOUS_BACKPACK_MENU.get(), BackpackScreen::new);
        event.register(Registration.ENDER_TREMENDOUS_BACKPACK_MENU.get(), BackpackScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TREMENDOUS_BACKPACK_BE_TYPE.get(), BackpackRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(), BackpackRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/backpack_body"));
        event.register(ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/backpack_flap"));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("curios")) return;
        try {
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                    Registration.TREMENDOUS_BACKPACK.get(), BackpackCurioRenderer::new);
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                    Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get(), BackpackCurioRenderer::new);
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
