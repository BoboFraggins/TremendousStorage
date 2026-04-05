package net.bobofraggins.intellistore.shared.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Adds an uncommon chance of manila folders to the village cartographer chest. */
public class VillageCartographerModifier extends LootModifier {

    public static final MapCodec<VillageCartographerModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, VillageCartographerModifier::new));

    public VillageCartographerModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        RandomSource rng = context.getRandom();
        if (rng.nextFloat() >= 0.25f) {
            return generatedLoot;
        }

        float roll = rng.nextFloat();
        StorageTier tier;
        if (roll < 0.50f) tier = StorageTier.COPPER;
        else if (roll < 0.80f) tier = StorageTier.IRON;
        else if (roll < 0.95f) tier = StorageTier.GOLD;
        else tier = StorageTier.DIAMOND;

        int count = 1 + rng.nextInt(3);
        ItemStack folder = new ItemStack(Registration.MANILA_FOLDER.get(), count);
        folder.set(
                Registration.FOLDER_CONTENTS.get(),
                net.bobofraggins.intellistore.storage.manillafolder.FolderContents.EMPTY.withTier(tier));
        generatedLoot.add(folder);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
