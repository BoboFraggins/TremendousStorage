package net.bobofraggins.tremendousstorage.power.stirlingengine;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only event subscriber for Stirling Engine rendering. */
public final class StirlingEngineClientEvents {

    private StirlingEngineClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.STIRLING_ENGINE_BE_TYPE.get(), StirlingEngineRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof StirlingEngineBlockEntity engine) {
                        return engine.getTier().getColor();
                    }
                    return -1;
                },
                Registration.STIRLING_ENGINE.get());
    }

    @SubscribeEvent
    public static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tremendousstorage", "storage_tier"),
                net.bobofraggins.tremendousstorage.shared.storage.StorageTierTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_body"));
        event.register(
                ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_flywheel"));
        event.register(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_piston"));
        event.register(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_bridge"));
    }
}
