package net.bobofraggins.tremendousstorage.storage.chest;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

/** Client-only event subscriber for Tremendous Chest rendering. */
public final class ChestClientEvents {

    private ChestClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TREMENDOUS_CHEST_BE_TYPE.get(), ChestRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), ChestRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                Identifier.fromNamespaceAndPath("tremendousstorage", "storage_tier"),
                net.bobofraggins.tremendousstorage.shared.storage.StorageTierTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterStandalone event) {
        event.register(
                ChestRenderer.BODY_MODEL_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/chest_body")));
        event.register(
                ChestRenderer.LID_MODEL_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/chest_lid")));
    }
}
