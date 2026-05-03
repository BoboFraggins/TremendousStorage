package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Registers color handlers and the GUI screen for the Barrel. */
public final class BarrelClientEvents {

    private BarrelClientEvents() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.BARREL_MENU.get(), BarrelScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0) return -1;
                    return state.getValue(BarrelBlock.TIER).getColor();
                },
                Registration.BARREL.get());
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
                Registration.BARREL_ITEM.get());
    }
}
