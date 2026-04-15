package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderSmithingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Smithing-table recipe: Picnic Basket + Ender Storage Upgrade → two linked Ender Picnic Baskets
 * sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>Re-applying the upgrade to an existing Ender Picnic Basket preserves the existing link ID,
 * producing a second basket linked to the same shared inventory.
 */
public class EnderPicnicBasketSmithingRecipe extends AbstractEnderSmithingRecipe {

    private static final EnderPicnicBasketSmithingRecipe INSTANCE = new EnderPicnicBasketSmithingRecipe();

    public static final MapCodec<EnderPicnicBasketSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderPicnicBasketSmithingRecipe> STREAM_CODEC =
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
        CustomData existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (existing == null) return -1L;
        CompoundTag tag = existing.copyTag();
        return tag.contains(EnderChestBlockEntity.TAG_LINK_ID) ? tag.getLong(EnderChestBlockEntity.TAG_LINK_ID) : -1L;
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_PICNIC_BASKET_ITEM.get());
        CustomData existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_PICNIC_BASKET_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_PICNIC_BASKET_SMITHING_RECIPE.get();
    }
}
