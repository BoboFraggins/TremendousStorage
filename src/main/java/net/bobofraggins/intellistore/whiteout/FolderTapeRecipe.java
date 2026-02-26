package net.bobofraggins.intellistore.whiteout;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.intellistore.manillafolder.FolderContents;
import net.bobofraggins.intellistore.manillafolder.ManillaFolderItem;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: Whiteout Tape + a locked-but-empty Manila Folder → unlocked folder.
 *
 * <p>The tape loses one durability per use via {@link #getRemainingItems}. When the tape
 * would exceed its max damage it is fully consumed (not returned as a broken item).
 *
 * <p>The recipe requires exactly two non-empty slots: one tape and one folder that is
 * locked (has a stored item type) but has a count of zero.
 */
public class FolderTapeRecipe extends CustomRecipe {

    public static final MapCodec<FolderTapeRecipe> CODEC =
            MapCodec.unit(new FolderTapeRecipe(CraftingBookCategory.MISC));

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderTapeRecipe> STREAM_CODEC =
            StreamCodec.unit(new FolderTapeRecipe(CraftingBookCategory.MISC));

    public FolderTapeRecipe(CraftingBookCategory category) {
        super(category);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ItemStack findTape(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof WhiteoutTapeItem) return s;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findFolder(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof ManillaFolderItem) return s;
        }
        return ItemStack.EMPTY;
    }

    /** True if the folder is locked (has a stored item type) but currently empty (count == 0). */
    private static boolean isLockedEmpty(ItemStack folder) {
        FolderContents contents = ManillaFolderItem.getContents(folder);
        return contents.storedItem().isPresent() && contents.count() == 0;
    }

    // -------------------------------------------------------------------------
    // CustomRecipe
    // -------------------------------------------------------------------------

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack tape = ItemStack.EMPTY;
        ItemStack folder = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;

            if (s.getItem() instanceof WhiteoutTapeItem) {
                if (!tape.isEmpty()) return false; // two tapes
                tape = s;
            } else if (s.getItem() instanceof ManillaFolderItem) {
                if (!folder.isEmpty()) return false; // two folders
                folder = s;
            } else {
                return false; // unexpected item
            }
        }

        if (tape.isEmpty() || folder.isEmpty()) return false;
        return isLockedEmpty(folder);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack folder = findFolder(input);
        if (folder.isEmpty()) return ItemStack.EMPTY;

        // Return the folder with contents cleared (storedItem = empty, count = 0)
        return ManillaFolderItem.setContents(folder.copyWithCount(1), FolderContents.EMPTY);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof WhiteoutTapeItem) {
                int newDamage = s.getDamageValue() + 1;
                if (newDamage < s.getMaxDamage()) {
                    ItemStack worn = s.copyWithCount(1);
                    worn.setDamageValue(newDamage);
                    remaining.set(i, worn);
                }
                // else tape is used up — leave slot empty (consumed)
                break;
            }
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.FOLDER_TAPE_RECIPE.get();
    }
}
