package net.bobofraggins.intellistore.shared.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Adds an uncommon chance of a random Lazurite tool to the village toolsmith chest. */
public class VillageToolsmithModifier extends LootModifier {

    public static final MapCodec<VillageToolsmithModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, VillageToolsmithModifier::new));

    public VillageToolsmithModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < 0.25f) {
            Item[] tools = {
                Registration.LAZURITE_PICKAXE.get(),
                Registration.LAZURITE_AXE.get(),
                Registration.LAZURITE_SHOVEL.get(),
                Registration.LAZURITE_HOE.get()
            };
            int index = context.getRandom().nextInt(tools.length);
            generatedLoot.add(new ItemStack(tools[index]));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
