package net.bobofraggins.tremendousstorage.glamping;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Game-bus event handlers for the Glamping Dimension. */
public class GlampingEvents {

    private GlampingEvents() {}

    /**
     * When a tent is crafted, set its custom name to "&lt;PlayerName&gt;'s Tent".
     *
     * <p>Using {@code CUSTOM_NAME} (not {@code ITEM_NAME}) means the name can be
     * changed or cleared on an anvil like any other named item.
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();
        if (!stack.is(GlampingRegistration.TENT.get().asItem())) return;

        String playerName = event.getEntity().getName().getString();
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(playerName + "'s Tent").withStyle(s -> s.withItalic(false)));
    }
}
