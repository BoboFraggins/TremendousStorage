package net.bobofraggins.intellistore.filingcabinet;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.register.Registration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only event subscriber for Filing Cabinet rendering registration. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FilingCabinetClientEvents {

    private FilingCabinetClientEvents() {}

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FilingCabinetModel.LAYER, FilingCabinetModel::createLayerDefinition);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.FILING_CABINET_BE_TYPE.get(), FilingCabinetRenderer::new);
    }
}
