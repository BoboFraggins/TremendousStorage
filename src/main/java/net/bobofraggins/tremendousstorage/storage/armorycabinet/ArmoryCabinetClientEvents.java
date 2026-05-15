package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

public final class ArmoryCabinetClientEvents {

    private ArmoryCabinetClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.ARMORY_CABINET_BE_TYPE.get(), ArmoryCabinetRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterStandalone event) {
        event.register(
                ArmoryCabinetRenderer.BODY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/armory_cabinet_body")));
        event.register(
                ArmoryCabinetRenderer.DOOR,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/armory_cabinet_door")));
        event.register(
                ArmoryCabinetRenderer.ARM_TOP,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/armory_cabinet_arm_top")));
        event.register(
                ArmoryCabinetRenderer.ARM_BOTTOM,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/armory_cabinet_arm_bottom")));
        event.register(
                ArmoryCabinetRenderer.WHEEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/armory_cabinet_wheel")));
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.ARMORY_CABINET_MENU.get(), ArmoryCabinetScreen::new);
    }
}
