package net.bobofraggins.tremendousstorage.storage.enderbarrel;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderCraftingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Crafting recipe: Barrel (any tier) + Ender Storage Upgrade →
 * two linked Ender Barrels sharing a freshly generated 64-bit {@code linkId}.
 */
public class EnderBarrelCraftingRecipe extends AbstractEnderCraftingRecipe {

    private static final EnderBarrelCraftingRecipe INSTANCE = new EnderBarrelCraftingRecipe();

    public static final MapCodec<EnderBarrelCraftingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderBarrelCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.BARREL_ITEM.get()
                || stack.getItem() == Registration.ENDER_BARREL_ITEM.get();
    }

    @Override
    protected boolean isAlreadyEnder(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_BARREL_ITEM.get();
    }

    @Override
    protected long getExistingLinkId(ItemStack stack) {
        var existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (existing == null) return -1L;
        CompoundTag tag = existing.getUnsafe();
        return tag.getLongOr(EnderBarrelBlockEntity.TAG_LINK_ID, -1L);
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_BARREL_ITEM.get());
        var existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.getUnsafe() : new CompoundTag();
        tag.putLong(EnderBarrelBlockEntity.TAG_LINK_ID, linkId);
        TagValueOutput _tagOut = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        _tagOut.store(tag);
        BlockItem.setBlockEntityData(result, Registration.ENDER_BARREL_BE_TYPE.get(), _tagOut);
        // Copy the barrel contents component so the ender barrel shows the same item/count
        var contents = base.get(Registration.BARREL_CONTENTS.get());
        if (contents != null) result.set(Registration.BARREL_CONTENTS.get(), contents);
        return result;
    }

    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_BARREL_ITEM.get());
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return Registration.ENDER_BARREL_CRAFTING_RECIPE.get();
    }
}
