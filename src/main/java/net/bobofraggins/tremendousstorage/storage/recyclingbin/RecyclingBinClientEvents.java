package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

public final class RecyclingBinClientEvents {

    private RecyclingBinClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.RECYCLING_BIN_BE_TYPE.get(), RecyclingBinRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterStandalone event) {
        event.register(
                RecyclingBinRenderer.BODY_MODEL_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_body")));
        event.register(
                RecyclingBinRenderer.LID_MODEL_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_lid")));
        event.register(
                RecyclingBinRenderer.PEDAL_MODEL_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_pedal")));
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.RECYCLING_BIN_MENU.get(), RecyclingBinScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath("tremendousstorage", "recycling_bin_renderer"),
                RecyclingBinItemRenderer.Unbaked.MAP_CODEC);
    }
}
