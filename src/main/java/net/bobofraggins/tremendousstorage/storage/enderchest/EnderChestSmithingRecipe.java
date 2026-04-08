package net.bobofraggins.tremendousstorage.storage.enderchest;

import java.security.SecureRandom;
import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderTremendousChestBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing-table recipe: Tremendous Chest (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Chests sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>All NBT data (tier, priority, crafting-upgrade flag, inventory) is copied from the
 * input chest to both outputs. The second chest is returned via {@link #getRemainingItems}
 * in place of the base-slot chest, exactly like an empty bucket remaining after a recipe.
 */
public class EnderChestSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<EnderChestSmithingRecipe> CODEC =
            MapCodec.unit(new EnderChestSmithingRecipe());

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderChestSmithingRecipe> STREAM_CODEC =
            StreamCodec.unit(new EnderChestSmithingRecipe());

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Thread-local carries the link ID from {@link #assemble} to {@link #getRemainingItems}
     * so both outputs share the same ID without regenerating it.
     */
    private static final ThreadLocal<long[]> PENDING_LINK =
            ThreadLocal.withInitial(() -> new long[] {-1L});

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.TREMENDOUS_CHEST_ITEM.get();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_STORAGE_UPGRADE.get();
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().isEmpty()
                && isBaseIngredient(input.base())
                && isAdditionIngredient(input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        long linkId = SECURE_RANDOM.nextLong();
        PENDING_LINK.get()[0] = linkId;
        return makeEnderChest(input.base(), linkId);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(SmithingRecipeInput input) {
        // Smithing slots: 0 = template, 1 = base, 2 = addition
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        long linkId = PENDING_LINK.get()[0];
        if (linkId != -1L) {
            PENDING_LINK.get()[0] = -1L;
            remaining.set(1, makeEnderChest(input.base(), linkId));
        }
        return remaining;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_CHEST_SMITHING_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static ItemStack makeEnderChest(ItemStack baseStack, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());

        // Copy all existing block_entity_data (tier, priority, crafting upgrade, inventory)
        CustomData existing = baseStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putLong(EnderTremendousChestBlockEntity.TAG_LINK_ID, linkId);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));

        return result;
    }
}
