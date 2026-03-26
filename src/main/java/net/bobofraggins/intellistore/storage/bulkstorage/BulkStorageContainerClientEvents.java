package net.bobofraggins.intellistore.storage.bulkstorage;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-only event subscriber for Bulk Storage Container rendering. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BulkStorageContainerClientEvents {

    private BulkStorageContainerClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                Registration.BULK_STORAGE_CONTAINER_BE_TYPE.get(), BulkStorageContainerRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Both body and lid are rendered by the BESR, so both must be registered as standalone models.
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/bulk_storage_container_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/bulk_storage_container_lid")));
    }
}
