package net.bobofraggins.tremendousstorage.shared.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Adds to the spawn bonus chest (if {@code bonusChestLootEnabled} is true):
 * - 25% chance: one Picnic Basket
 * - Always: 4–8 s'mores
 */
public class SpawnBonusChestModifier extends LootModifier {

    public static final MapCodec<SpawnBonusChestModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, SpawnBonusChestModifier::new));

    public SpawnBonusChestModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!TremendousStorageConfig.BONUS_CHEST_LOOT_ENABLED.get()) {
            return generatedLoot;
        }

        RandomSource rng = context.getRandom();

        if (rng.nextFloat() < 0.25f) {
            generatedLoot.add(new ItemStack(Registration.PICNIC_BASKET_ITEM.get()));
        }

        int smoreCount = 4 + rng.nextInt(5); // 4–8
        generatedLoot.add(new ItemStack(Registration.SMORE.get(), smoreCount));

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
