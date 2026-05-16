package net.bobofraggins.tremendousstorage.glamping.magichat;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Gives a random 5% of naturally-spawning zombies a Magic Hat containing a surprise mob. */
public final class MagicHatZombieHandler {

    private MagicHatZombieHandler() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        EntitySpawnReason spawnType = event.getSpawnType();
        if (spawnType == EntitySpawnReason.COMMAND
                || spawnType == EntitySpawnReason.SPAWN_ITEM_USE
                || spawnType == EntitySpawnReason.BUCKET
                || spawnType == EntitySpawnReason.DISPENSER) return;

        if (event.getLevel().getRandom().nextFloat() >= 0.05f) return;

        EntityType<?> mobType = pickMobType(event.getLevel().getRandom());
        Level level = zombie.level();
        Entity mob = mobType.create(level, EntitySpawnReason.TRIGGERED);
        if (mob == null) return;

        TagValueOutput _tagOut = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        if (!mob.save(_tagOut)) return;
        CompoundTag mobTag = _tagOut.buildResult();

        CompoundTag wrapper = new CompoundTag();
        wrapper.put(MagicHatItem.MOB_KEY, mobTag);

        ItemStack hat = new ItemStack(Registration.MAGIC_HAT_ITEM.get());
        hat.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));

        zombie.setItemSlot(EquipmentSlot.HEAD, hat);
        zombie.setDropChance(EquipmentSlot.HEAD, 1.0f);
    }

    private static EntityType<?> pickMobType(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.50f) return EntityType.RABBIT;
        if (r < 0.75f) return EntityType.CHICKEN;
        if (r < 0.80f) return EntityType.SHEEP;
        if (r < 0.85f) return EntityType.COW;
        if (r < 0.90f) return EntityType.CAT;
        if (r < 0.95f) return EntityType.VILLAGER;
        return EntityType.RAVAGER;
    }
}
