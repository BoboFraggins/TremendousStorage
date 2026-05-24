package net.bobofraggins.tremendousstorage.glamping;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

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

        Component playerName = event.getEntity().getName();
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable("event.tremendousstorage.tent_name", playerName)
                        .withStyle(s -> s.withItalic(false)));
    }

    /** Beds in the Glamping Dimension are always usable, regardless of time of day or nearby mobs. */
    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (event.getEntity().level().dimension().equals(GlampingDimension.KEY)) {
            event.setProblem(null);
        }
    }

    /** Keep sleeping through the night (or day) in the Glamping Dimension. */
    @SubscribeEvent
    public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        if (event.getEntity().level().dimension().equals(GlampingDimension.KEY)) {
            event.setContinueSleeping(true);
        }
    }

    /**
     * When a player wakes up from sleeping in the Glamping Dimension, reset the overworld to
     * daybreak (time 0) so they return to a fresh morning.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(GlampingDimension.KEY)) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        if (server == null) return;
        net.minecraft.world.clock.ServerClockManager clockManager = server.clockManager();
        var overworldClock = server.registryAccess().getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
        clockManager.moveToTimeMarker(overworldClock, net.minecraft.world.clock.ClockTimeMarkers.WAKE_UP_FROM_SLEEP);
    }
}
