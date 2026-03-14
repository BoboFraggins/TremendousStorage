package net.bobofraggins.intellistore.storage.items;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Applies Resistance II (3 minutes) to any player standing in Healing Salve fluid.
 * The effect is re-applied every 5 ticks so it stays close to full duration while
 * the player remains in the fluid.
 */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class HealingSalveEffectHandler {

    private HealingSalveEffectHandler() {}

    /** Duration in ticks (3 minutes). */
    private static final int DURATION = 3 * 60 * 20;

    /** Re-apply interval in ticks. */
    private static final int INTERVAL = 5;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % INTERVAL != 0) return;

        if (player.isInFluidType(Registration.HEALING_SALVE_TYPE.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION, 1, false, true, true));
        }
    }
}
