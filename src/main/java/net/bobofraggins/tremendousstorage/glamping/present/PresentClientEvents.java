package net.bobofraggins.tremendousstorage.glamping.present;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

public final class PresentClientEvents {

    private PresentClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.PRESENT_BE_TYPE.get(), PresentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterStandalone event) {
        event.register(
                PresentRenderer.MODEL_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath("tremendousstorage", "block/present")));
    }
}
