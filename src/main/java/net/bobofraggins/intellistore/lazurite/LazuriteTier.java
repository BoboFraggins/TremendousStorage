package net.bobofraggins.intellistore.lazurite;

import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum LazuriteTier implements Tier {
    INSTANCE,
    PAXEL;

    @Override
    public int getUses() {
        return this == PAXEL ? 2048 : 1024;
    }

    @Override
    public float getSpeed() {
        return 4.0f;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0f;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "incorrect_for_lazurite_tool"));
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(net.bobofraggins.intellistore.shared.register.Registration.LAZURITE_INGOT.get());
    }
}
