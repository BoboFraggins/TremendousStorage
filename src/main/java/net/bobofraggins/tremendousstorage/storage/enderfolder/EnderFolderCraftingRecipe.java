package net.bobofraggins.tremendousstorage.storage.enderfolder;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.recipe.AbstractEnderCraftingRecipe;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Crafting recipe: Manila Folder (any tier) + Ender Storage Upgrade →
 * two linked Ender Folders sharing a freshly generated 64-bit {@code linkId}.
 *
 * <p>Template slot is left empty. Base slot holds the Manila Folder; addition slot
 * holds the Ender Storage Upgrade. The primary Ender Folder is the result item;
 * the second linked Ender Folder is returned via {@link #getRemainingItems} in place
 * of the base-slot folder, exactly like an empty bucket remaining after a water-bucket
 * recipe.
 */
public class EnderFolderCraftingRecipe extends AbstractEnderCraftingRecipe {

    private static final EnderFolderCraftingRecipe INSTANCE = new EnderFolderCraftingRecipe();

    public static final MapCodec<EnderFolderCraftingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderFolderCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() instanceof ManillaFolderItem;
    }

    @Override
    protected boolean isAlreadyEnder(ItemStack stack) {
        return stack.getItem() instanceof EnderFolderItem;
    }

    @Override
    protected long getExistingLinkId(ItemStack stack) {
        return EnderFolderItem.getLinkId(stack);
    }

    @Override
    protected ItemStack makeEnderItem(ItemStack base, long linkId) {
        FolderContents contents = ManillaFolderItem.getContents(base);
        ItemStack result = new ItemStack(Registration.ENDER_FOLDER.get());
        result.set(Registration.FOLDER_CONTENTS.get(), contents);
        EnderFolderItem.setLinkId(result, linkId);
        return result;
    }

    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.ENDER_FOLDER.get());
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return Registration.ENDER_FOLDER_CRAFTING_RECIPE.get();
    }
}
