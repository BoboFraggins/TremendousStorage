package net.bobofraggins.tremendousstorage.storage.endertank;

import java.security.SecureRandom;
import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
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
 * Smithing-table recipe: Tremendous Tank (any tier) + Ender Storage Upgrade →
 * two linked Ender Tremendous Tanks sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>The BED (tier, voidExcess) and the {@link Registration#TREMENDOUS_TANK_CONTENTS} component
 * are copied from the input tank to both outputs. The second tank is returned via
 * {@link #getRemainingItems} in the base slot, as with vanilla smithing remainder items.
 */
public class EnderTankSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<EnderTankSmithingRecipe> CODEC =
            MapCodec.unit(new EnderTankSmithingRecipe());

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderTankSmithingRecipe> STREAM_CODEC =
            StreamCodec.unit(new EnderTankSmithingRecipe());

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Passes the link ID from {@link #assemble} to {@link #getRemainingItems}. */
    private static final ThreadLocal<long[]> PENDING_LINK =
            ThreadLocal.withInitial(() -> new long[] {-1L});

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == Registration.TREMENDOUS_TANK_ITEM.get();
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
        return makeEnderTank(input.base(), linkId);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(SmithingRecipeInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        long linkId = PENDING_LINK.get()[0];
        if (linkId != -1L) {
            PENDING_LINK.get()[0] = -1L;
            remaining.set(1, makeEnderTank(input.base(), linkId));
        }
        return remaining;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_TREMENDOUS_TANK_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_TANK_SMITHING_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static ItemStack makeEnderTank(ItemStack baseStack, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_TREMENDOUS_TANK_ITEM.get());

        // Copy BED (tier, voidExcess) and add the link ID
        CustomData existing = baseStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putLong(EnderTremendousTankBlockEntity.TAG_LINK_ID, linkId);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));

        // Copy fluid contents component so both tanks start with the same fluid
        var contents = baseStack.get(Registration.TREMENDOUS_TANK_CONTENTS.get());
        if (contents != null) {
            result.set(Registration.TREMENDOUS_TANK_CONTENTS.get(), contents);
        }

        return result;
    }
}
