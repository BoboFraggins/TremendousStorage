package net.bobofraggins.tremendousstorage.canvas;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CanvasClientEvents {

    private CanvasClientEvents() {}

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "canvas"), CanvasModelLoader.INSTANCE);
    }
}
