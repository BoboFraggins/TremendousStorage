package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RecyclingBinClientEvents {

    private RecyclingBinClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.RECYCLING_BIN_BE_TYPE.get(), RecyclingBinRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_lid")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/recycling_bin_pedal")));
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.RECYCLING_BIN_MENU.get(), RecyclingBinScreen::new);
    }
}
