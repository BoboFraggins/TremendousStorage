package net.bobofraggins.tremendousstorage.storage.enderchest;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderCraftingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
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
 * Crafting recipe: Tremendous Chest (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Chests sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>All NBT data (tier, priority, crafting-upgrade flag, inventory) is copied from the
 * input chest to both outputs. The second chest is returned via {@link #getRemainingItems}
 * in place of the base-slot chest, exactly like an empty bucket remaining after a recipe.
 */
public class EnderChestCraftingRecipe extends AbstractEnderCraftingRecipe {

    private static final EnderChestCraftingRecipe INSTANCE = new EnderChestCraftingRecipe();

    public static final MapCodec<EnderChestCraftingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderChestCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.TREMENDOUS_CHEST_ITEM.get()
                || stack.getItem() == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get();
    }

    @Override
    protected boolean isAlreadyEnder(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get();
    }

    @Override
    protected long getExistingLinkId(ItemStack stack) {
        var existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (existing == null) return -1L;
        CompoundTag tag = existing.copyTagWithoutId();
        return tag.getLongOr(EnderChestBlockEntity.TAG_LINK_ID, -1L);
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
        var existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTagWithoutId() : new CompoundTag();
        tag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);
        TagValueOutput _tagOut = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        _tagOut.store(tag);
        BlockItem.setBlockEntityData(result, Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), _tagOut);
        return result;
    }

    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return Registration.ENDER_CHEST_CRAFTING_RECIPE.get();
    }
}
