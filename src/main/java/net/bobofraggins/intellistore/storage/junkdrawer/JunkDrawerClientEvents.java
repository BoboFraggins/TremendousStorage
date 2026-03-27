package net.bobofraggins.intellistore.storage.junkdrawer;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
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
        // Both body and door are rendered by the BESR, so both must be registered as standalone models.
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/junk_drawer_body")));
        event.register(JunkDrawerRenderer.DOOR_MODEL);
    }
}
