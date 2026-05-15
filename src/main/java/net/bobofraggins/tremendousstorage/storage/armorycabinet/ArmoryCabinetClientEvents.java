package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ArmoryCabinetClientEvents {

    private ArmoryCabinetClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.ARMORY_CABINET_BE_TYPE.get(), ArmoryCabinetRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (String part : new String[] {
            "armory_cabinet_body",
            "armory_cabinet_door",
            "armory_cabinet_arm_top",
            "armory_cabinet_arm_bottom",
            "armory_cabinet_wheel"
        }) {
            event.register(ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/" + part));
        }
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.ARMORY_CABINET_MENU.get(), ArmoryCabinetScreen::new);
    }
}
