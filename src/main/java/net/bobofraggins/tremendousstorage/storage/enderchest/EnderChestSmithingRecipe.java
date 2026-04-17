package net.bobofraggins.tremendousstorage.storage.enderchest;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderSmithingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Smithing-table recipe: Tremendous Chest (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Chests sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>All NBT data (tier, priority, crafting-upgrade flag, inventory) is copied from the
 * input chest to both outputs. The second chest is returned via {@link #getRemainingItems}
 * in place of the base-slot chest, exactly like an empty bucket remaining after a recipe.
 */
public class EnderChestSmithingRecipe extends AbstractEnderSmithingRecipe {

    private static final EnderChestSmithingRecipe INSTANCE = new EnderChestSmithingRecipe();

    public static final MapCodec<EnderChestSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderChestSmithingRecipe> STREAM_CODEC =
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
        CustomData existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (existing == null) return -1L;
        CompoundTag tag = existing.copyTag();
        return tag.contains(EnderChestBlockEntity.TAG_LINK_ID) ? tag.getLong(EnderChestBlockEntity.TAG_LINK_ID) : -1L;
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
        CustomData existing = base.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);
        BlockItem.setBlockEntityData(result, Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get(), tag);
        return result;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_CHEST_SMITHING_RECIPE.get();
    }
}
