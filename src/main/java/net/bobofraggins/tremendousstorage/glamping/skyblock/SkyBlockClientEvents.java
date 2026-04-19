package net.bobofraggins.tremendousstorage.glamping.skyblock;

import net.bobofraggins.tremendousstorage.glamping.GlampingRegistration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class SkyBlockClientEvents {

    private SkyBlockClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GlampingRegistration.SKY_BLOCK_BE_TYPE.get(), SkyBlockRenderer::new);
    }
}
