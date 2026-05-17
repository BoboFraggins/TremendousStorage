package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderCraftingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
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
 * Crafting recipe: Picnic Basket + Ender Storage Upgrade → two linked Ender Picnic Baskets
 * sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>Re-applying the upgrade to an existing Ender Picnic Basket preserves the existing link ID,
 * producing a second basket linked to the same shared inventory.
 */
public class EnderPicnicBasketCraftingRecipe extends AbstractEnderCraftingRecipe {

    private static final EnderPicnicBasketCraftingRecipe INSTANCE = new EnderPicnicBasketCraftingRecipe();

    public static final MapCodec<EnderPicnicBasketCraftingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderPicnicBasketCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.PICNIC_BASKET_ITEM.get()
                || stack.getItem() == Registration.ENDER_PICNIC_BASKET_ITEM.get();
    }

    @Override
    protected boolean isAlreadyEnder(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_PICNIC_BASKET_ITEM.get();
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
        ItemStack result = new ItemStack(Registration.ENDER_PICNIC_BASKET_ITEM.get());
        var existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTagWithoutId() : new CompoundTag();
        tag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);
        TagValueOutput _tagOut = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        _tagOut.store(tag);
        BlockItem.setBlockEntityData(result, Registration.ENDER_PICNIC_BASKET_BE_TYPE.get(), _tagOut);
        return result;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.ENDER_PICNIC_BASKET_CRAFTING_RECIPE
                .get();
    }

    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_PICNIC_BASKET_ITEM.get());
    }
}
