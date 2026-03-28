package net.bobofraggins.intellistore.power.stirlingengine;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only event subscriber for Stirling Engine rendering. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
                    return StorageTier.PAPER.getColor();
                },
                Registration.STIRLING_ENGINE_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/stirling_engine_body")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/stirling_engine_flywheel")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/stirling_engine_piston")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("intellistore", "block/stirling_engine_bridge")));
    }
}
