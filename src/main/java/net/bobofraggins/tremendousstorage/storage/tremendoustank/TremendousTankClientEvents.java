package net.bobofraggins.tremendousstorage.storage.tremendoustank;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-side event subscriber that registers the Tremendous Tank's block entity renderer. */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TremendousTankClientEvents {

    private TremendousTankClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TREMENDOUS_TANK_BE_TYPE.get(), TremendousTankRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_TREMENDOUS_TANK_BE_TYPE.get(), TremendousTankRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    if (level.getBlockEntity(pos) instanceof TremendousTankBlockEntity be) {
                        return be.getTier().getColor();
                    }
                    return StorageTier.WOOD.getColor();
                },
                Registration.TREMENDOUS_TANK.get(),
                Registration.ENDER_TREMENDOUS_TANK.get());
    }
}
