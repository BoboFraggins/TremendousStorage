package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import com.mojang.serialization.MapCodec;
import java.security.SecureRandom;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
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
 * Smithing-table recipe: Tremendous Backpack (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Backpacks sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>All data (tier, priority, sort mode, crafting-upgrade flag, inventory) is copied from the
 * input backpack to both outputs via the {@link BackpackContents} data component.
 * The second backpack is returned via {@link #getRemainingItems} in the base slot.
 */
public class EnderBackpackSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<EnderBackpackSmithingRecipe> CODEC = MapCodec.unit(new EnderBackpackSmithingRecipe());

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderBackpackSmithingRecipe> STREAM_CODEC =
            StreamCodec.unit(new EnderBackpackSmithingRecipe());

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final ThreadLocal<long[]> PENDING_LINK = ThreadLocal.withInitial(() -> new long[] {-1L});

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.TREMENDOUS_BACKPACK.get()
                || stack.getItem() == Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_STORAGE_UPGRADE.get();
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().isEmpty() && isBaseIngredient(input.base()) && isAdditionIngredient(input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        long linkId;
        if (input.base().getItem() == Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get()) {
            Long existing = input.base().get(Registration.ENDER_LINK_ID.get());
            linkId = existing != null ? existing : SECURE_RANDOM.nextLong();
        } else {
            linkId = SECURE_RANDOM.nextLong();
        }
        PENDING_LINK.get()[0] = linkId;
        return makeEnderBackpack(input.base(), linkId);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(SmithingRecipeInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        long linkId = PENDING_LINK.get()[0];
        if (linkId != -1L) {
            PENDING_LINK.get()[0] = -1L;
            if (input.base().getItem() == Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get()) {
                remaining.set(1, input.base().copy());
            } else {
                remaining.set(1, makeEnderBackpack(input.base(), linkId));
            }
        }
        return remaining;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_BACKPACK_SMITHING_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static ItemStack makeEnderBackpack(ItemStack baseStack, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get());

        // Copy BackpackContents (item-form inventory, tier, priority, etc.)
        BackpackContents contents = baseStack.get(Registration.TREMENDOUS_BACKPACK_CONTENTS.get());
        if (contents != null) {
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
        }

        // Copy or create block_entity_data (for when the ender backpack is placed as a block)
        CustomData existing = baseStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        // If no block_entity_data but BackpackContents is present, seed it
        if (existing == null && contents != null) {
            tag.putString("Tier", contents.tier().getId());
            tag.putInt("Priority", contents.priority().ordinal());
            tag.putString("SortMode", contents.sortMode().name());
            if (contents.hasCraftingUpgrade()) tag.putBoolean("CraftingUpgrade", true);
        }
        tag.putLong(EnderChestBlockEntity.TAG_LINK_ID, linkId);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));

        // Set link ID as a standalone component for fast access in item-form openUi
        result.set(Registration.ENDER_LINK_ID.get(), linkId);

        return result;
    }
}
