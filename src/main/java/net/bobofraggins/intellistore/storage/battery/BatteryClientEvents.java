package net.bobofraggins.intellistore.storage.battery;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-side event subscriber that registers the Battery's block entity renderer and tint. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BatteryClientEvents {

    private BatteryClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.BATTERY_BE_TYPE.get(), BatteryRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    if (level.getBlockEntity(pos) instanceof BatteryBlockEntity be) {
                        return be.getTier().getColor();
                    }
                    return StorageTier.PAPER.getColor();
                },
                Registration.BATTERY.get());
    }
}
