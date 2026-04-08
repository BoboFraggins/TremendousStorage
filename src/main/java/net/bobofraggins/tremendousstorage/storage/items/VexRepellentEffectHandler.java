package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Every {@value #VEX_CHECK_INTERVAL} ticks, finds all Vexes within
 * {@value #VEX_RANGE} blocks of a player bearing the Vex Repellent effect and
 * launches them away at speed {@value #REPEL_SPEED}.
 */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VexRepellentEffectHandler {

    private VexRepellentEffectHandler() {}

    private static final int VEX_CHECK_INTERVAL = 10;
    private static final double VEX_RANGE = 5.0;
    private static final double REPEL_SPEED = 3.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!player.hasEffect(Registration.VEX_REPELLENT_EFFECT)) return;
        if (player.tickCount % VEX_CHECK_INTERVAL != 0) return;

        AABB searchBox = player.getBoundingBox().inflate(VEX_RANGE);
        List<Vex> vexes = player.level().getEntitiesOfClass(Vex.class, searchBox);
        for (Vex vex : vexes) {
            Vec3 away = vex.position().subtract(player.position());
            if (away.lengthSqr() < 1e-6) {
                away = new Vec3(1.0, 0.0, 0.0);
            }
            vex.setDeltaMovement(away.normalize().scale(REPEL_SPEED));
            vex.hurtMarked = true;
        }
    }
}
