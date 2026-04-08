package net.bobofraggins.tremendousstorage.storage.tremendousbackpack;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Tremendous Backpack — keybind, screen, BESR, and worn layer. */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TremendousBackpackClientEvents {

    private TremendousBackpackClientEvents() {}

    /** Keybind to open the Tremendous Backpack UI. Default: B. */
    public static KeyMapping OPEN_TREMENDOUS_BACKPACK;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_TREMENDOUS_BACKPACK = new KeyMapping(
                "key.tremendousstorage.open_tremendous_backpack",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.tremendousstorage");
        event.register(OPEN_TREMENDOUS_BACKPACK);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.TREMENDOUS_BACKPACK_MENU.get(), TremendousBackpackScreen::new);
        event.register(Registration.ENDER_TREMENDOUS_BACKPACK_MENU.get(), TremendousBackpackScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                Registration.TREMENDOUS_BACKPACK_BE_TYPE.get(), TremendousBackpackRenderer::new);
        event.registerBlockEntityRenderer(
                Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(), TremendousBackpackRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/tremendous_backpack_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/tremendous_backpack_flap")));
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer pr) {
                pr.addLayer(new TremendousBackpackWornLayer(pr));
            }
        }
    }
}
