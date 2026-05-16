package net.bobofraggins.tremendousstorage.power.stirlingengine;

import java.util.List;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

/** Client-only event subscriber for Stirling Engine rendering. */
public final class StirlingEngineClientEvents {

    private StirlingEngineClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.STIRLING_ENGINE_BE_TYPE.get(), StirlingEngineRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                List.of(new BlockTintSource() {
                    @Override
                    public int color(net.minecraft.world.level.block.state.BlockState state) {
                        return -1;
                    }

                    @Override
                    public int colorInWorld(
                            net.minecraft.world.level.block.state.BlockState state,
                            BlockAndTintGetter level,
                            BlockPos pos) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof StirlingEngineBlockEntity engine) {
                            return engine.getTier().getColor();
                        }
                        return -1;
                    }
                }),
                Registration.STIRLING_ENGINE.get());
    }

    @SubscribeEvent
    public static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                Identifier.fromNamespaceAndPath("tremendousstorage", "storage_tier"),
                net.bobofraggins.tremendousstorage.shared.storage.StorageTierTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterStandalone event) {
        event.register(
                StirlingEngineRenderer.BODY_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_body")));
        event.register(
                StirlingEngineRenderer.FLYWHEEL_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_flywheel")));
        event.register(
                StirlingEngineRenderer.PISTON_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_piston")));
        event.register(
                StirlingEngineRenderer.BRIDGE_MODEL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "block/stirling_engine_bridge")));
    }
}
