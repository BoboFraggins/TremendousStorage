package net.bobofraggins.intellistore.storage.junkdrawer;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-only event subscriber for Junk Drawer rendering. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JunkDrawerClientEvents {

    private JunkDrawerClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.JUNK_DRAWER_BE_TYPE.get(), JunkDrawerRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Body model is loaded automatically via the blockstate JSON.
        // Door model must be registered here since the BESR looks it up by standalone location.
        event.register(JunkDrawerRenderer.DOOR_MODEL);
    }
}
