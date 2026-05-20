package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.tremendousstorage.shared.input.TremendousStorageKeys;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

/** Client-only mod-bus events for the Picnic Basket — keybind, screen, BESR, and model registration. */
public final class PicnicBasketClientEvents {

    private PicnicBasketClientEvents() {}

    /** Keybind to open the Picnic Basket UI. Default: unbound. */
    public static KeyMapping OPEN_PICNIC_BASKET;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_PICNIC_BASKET = new KeyMapping(
                "key.tremendousstorage.open_picnic_basket",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                TremendousStorageKeys.CATEGORY);
        event.register(OPEN_PICNIC_BASKET);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.PICNIC_BASKET_ITEM_MENU.get(), PicnicBasketItemScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.PICNIC_BASKET_BE_TYPE.get(), PicnicBasketRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_PICNIC_BASKET_BE_TYPE.get(), PicnicBasketRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterStandalone event) {
        event.register(
                PicnicBasketRenderer.BODY_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_body")));
        event.register(
                PicnicBasketRenderer.LEFT_LID_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_left_lid")));
        event.register(
                PicnicBasketRenderer.RIGHT_LID_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_right_lid")));
    }
}
