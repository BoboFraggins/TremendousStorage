package net.bobofraggins.tremendousstorage.shared.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Adds a 1% chance of finding a Brain, Zombie Brain, or Positive Vibes Bucket
 * in any vanilla chest loot table.
 */
public class AnyChestModifier extends LootModifier {

    public static final MapCodec<AnyChestModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, AnyChestModifier::new));

    public AnyChestModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.getQueriedLootTableId().getPath().startsWith("chests/")) {
            return generatedLoot;
        }

        RandomSource rng = context.getRandom();
        if (rng.nextFloat() < 0.03f) {
            Item[] pool = {
                Registration.BRAIN.get(), Registration.ZOMBIE_BRAIN.get(), Registration.POSITIVE_VIBES_BUCKET.get()
            };
            generatedLoot.add(new ItemStack(pool[rng.nextInt(pool.length)]));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
