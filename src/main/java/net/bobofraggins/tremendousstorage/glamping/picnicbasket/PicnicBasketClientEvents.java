package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-only mod-bus events for the Picnic Basket — BESR and model registration. */
public final class PicnicBasketClientEvents {

    private PicnicBasketClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.PICNIC_BASKET_BE_TYPE.get(), PicnicBasketRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_PICNIC_BASKET_BE_TYPE.get(), PicnicBasketRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_left_lid")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/picnic_basket_right_lid")));
    }
}
