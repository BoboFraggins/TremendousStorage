package net.bobofraggins.tremendousstorage.storage.enderfolder;

import java.security.SecureRandom;
import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing-table recipe: Manila Folder (any tier) + Ender Storage Upgrade →
 * two linked Ender Folders sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>Template slot is left empty. Base slot holds the Manila Folder; addition slot
 * holds the Ender Storage Upgrade. The primary Ender Folder is the result item;
 * the second linked Ender Folder is returned via {@link #getRemainingItems} in place
 * of the base-slot folder, exactly like an empty bucket remaining after a water-bucket
 * recipe.
 */
public class EnderFolderSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<EnderFolderSmithingRecipe> CODEC =
            MapCodec.unit(new EnderFolderSmithingRecipe());

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderFolderSmithingRecipe> STREAM_CODEC =
            StreamCodec.unit(new EnderFolderSmithingRecipe());

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Carries the link ID generated in {@link #assemble} so that {@link #getRemainingItems}
     * can attach the same ID to the second output folder without regenerating it.
     *
     * <p>This is safe because crafting calls {@code assemble} then {@code getRemainingItems}
     * on the same thread without interleaving.
     */
    private static final ThreadLocal<long[]> PENDING_LINK =
            ThreadLocal.withInitial(() -> new long[] {-1L});

    // -------------------------------------------------------------------------
    // Ingredient predicates
    // -------------------------------------------------------------------------

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() instanceof ManillaFolderItem
                && !(stack.getItem() instanceof EnderFolderItem);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_STORAGE_UPGRADE.get();
    }

    // -------------------------------------------------------------------------
    // Recipe matching
    // -------------------------------------------------------------------------

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().isEmpty()
                && isBaseIngredient(input.base())
                && isAdditionIngredient(input.addition());
    }

    // -------------------------------------------------------------------------
    // Assembly — primary output (first Ender Folder)
    // -------------------------------------------------------------------------

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        long linkId = SECURE_RANDOM.nextLong();
        PENDING_LINK.get()[0] = linkId;
        return makeEnderFolder(input.base(), linkId);
    }

    // -------------------------------------------------------------------------
    // Remaining items — second Ender Folder replaces the base-slot folder
    // -------------------------------------------------------------------------

    @Override
    public NonNullList<ItemStack> getRemainingItems(SmithingRecipeInput input) {
        // Smithing input slots: 0 = template, 1 = base, 2 = addition
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        long linkId = PENDING_LINK.get()[0];
        if (linkId == -1L) return remaining;
        PENDING_LINK.get()[0] = -1L; // consume

        remaining.set(1, makeEnderFolder(input.base(), linkId));
        return remaining;
    }

    // -------------------------------------------------------------------------
    // Serializer / result
    // -------------------------------------------------------------------------

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_FOLDER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_FOLDER_SMITHING_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static ItemStack makeEnderFolder(ItemStack baseFolder, long linkId) {
        FolderContents contents = ManillaFolderItem.getContents(baseFolder);
        ItemStack result = new ItemStack(Registration.ENDER_FOLDER.get());
        result.set(Registration.FOLDER_CONTENTS.get(), contents);
        EnderFolderItem.setLinkId(result, linkId);
        return result;
    }
}
