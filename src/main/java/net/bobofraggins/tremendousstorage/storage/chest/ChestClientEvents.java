package net.bobofraggins.tremendousstorage.storage.chest;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only event subscriber for Tremendous Chest rendering. */
public final class ChestClientEvents {

    private ChestClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TREMENDOUS_CHEST_BE_TYPE.get(), ChestRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), ChestRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex != 0) return -1;
                    var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.getUnsafe();
                        if (tag.contains("Tier")) {
                            return StorageTier.fromId(tag.getString("Tier")).getColor();
                        }
                    }
                    return StorageTier.WOOD.getColor();
                },
                Registration.TREMENDOUS_CHEST_ITEM.get(),
                Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Both body and lid are rendered by the BESR, so both must be registered as standalone models.
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/chest_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/chest_lid")));
    }
}
