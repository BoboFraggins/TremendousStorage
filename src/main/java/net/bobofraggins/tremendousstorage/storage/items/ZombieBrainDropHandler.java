package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Adds a 1-in-8 chance for any Zombie subtype to drop a Zombie Brain on death.
 *
 * <p>Zombie covers ZombieVillager, Husk, Drowned, and all other Zombie subclasses.
 */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ZombieBrainDropHandler {

    private ZombieBrainDropHandler() {}

    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie)) return;
        Level level = event.getEntity().level();
        if (level.random.nextInt(8) != 0) return;
        BlockPos pos = event.getEntity().blockPosition();
        event.getDrops()
                .add(new ItemEntity(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        new ItemStack(Registration.ZOMBIE_BRAIN.get())));
    }
}
