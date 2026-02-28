package net.bobofraggins.intellistore.power.stirlingengine;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registration for the Stirling Engine block entity renderer. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StirlingEngineClientEvents {

    private StirlingEngineClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.STIRLING_ENGINE_BE_TYPE.get(), StirlingEngineRenderer::new);
    }
}
