package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Applies Regeneration I (3 minutes) to any player touching Positive Vibes fluid.
 * The effect is re-applied every 5 ticks so it stays close to full duration while
 * the player remains in the fluid.
 */
public final class PositiveVibesEffectHandler {

    private PositiveVibesEffectHandler() {}

    /** Duration in ticks (3 minutes). */
    private static final int DURATION = 3 * 60 * 20;

    /** Re-apply interval in ticks. */
    private static final int INTERVAL = 5;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % INTERVAL != 0) return;

        if (isInVibes(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION, 0, false, true, true));
            if (player.isOnFire()) {
                player.clearFire();
            }
        }
    }

    private static boolean isInVibes(Player player) {
        FluidType vibes = Registration.POSITIVE_VIBES_TYPE.get();
        // Check feet block (wading) and the block containing the body centre (swimming/submerged).
        BlockPos feet = player.blockPosition();
        BlockPos body = BlockPos.containing(player.position().add(0, player.getBbHeight() / 2.0, 0));
        return player.level().getFluidState(feet).getFluidType() == vibes
                || player.level().getFluidState(body).getFluidType() == vibes;
    }
}
