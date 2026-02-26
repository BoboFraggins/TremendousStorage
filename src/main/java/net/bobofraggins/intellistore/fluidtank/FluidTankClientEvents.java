package net.bobofraggins.intellistore.fluidtank;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.register.Registration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-side event subscriber that registers the Fluid Tank's block entity renderer. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidTankClientEvents {

    private FluidTankClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.FLUID_TANK_BE_TYPE.get(), FluidTankRenderer::new);
    }
}
