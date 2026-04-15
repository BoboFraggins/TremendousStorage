package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-side event subscriber that registers the Tank's block entity renderer. */
public final class TankClientEvents {

    private TankClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TANK_BE_TYPE.get(), TankRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_TANK_BE_TYPE.get(), TankRenderer::new);
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
                Registration.TANK_ITEM.get(),
                Registration.ENDER_TANK_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) return -1;
                    if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                        return be.getTier().getColor();
                    }
                    return StorageTier.WOOD.getColor();
                },
                Registration.TANK.get(),
                Registration.ENDER_TANK.get());
    }
}
