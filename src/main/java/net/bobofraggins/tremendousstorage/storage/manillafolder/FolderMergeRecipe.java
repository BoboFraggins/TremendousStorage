package net.bobofraggins.tremendousstorage.storage.manillafolder;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting grid recipe: two Manila Folders of the same tier and same stored item → one folder
 * with combined count (capped at capacity).
 *
 * <p>Both folders must be locked to the same item type. Any overflow is silently dropped — the
 * player should check capacities before merging.
 */
public class FolderMergeRecipe extends CustomRecipe {

    public FolderMergeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack[] pair = findPair(input);
        if (pair == null) return false;

        FolderContents c1 = ManillaFolderItem.getContents(pair[0]);
        FolderContents c2 = ManillaFolderItem.getContents(pair[1]);

        if (c1.isEmpty() || c2.isEmpty()) return false;
        if (c1.tier() != c2.tier()) return false;

        return ItemStack.isSameItemSameComponents(
                c1.storedItem().get(), c2.storedItem().get());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] pair = findPair(input);
        if (pair == null) return ItemStack.EMPTY;

        ItemStack base = pair[0];
        ItemStack other = pair[1];

        FolderContents c1 = ManillaFolderItem.getContents(base);
        FolderContents c2 = ManillaFolderItem.getContents(other);

        if (c1.isEmpty() || c2.isEmpty()) return ItemStack.EMPTY;

        long capacity = ManillaFolderItem.getCapacity(base);
        FolderContents.InsertResult result = c1.insert(c2.count(), capacity);

        return ManillaFolderItem.setContents(base.copyWithCount(1), result.updated());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.FOLDER_MERGE_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns exactly two Manila Folder stacks if the grid contains nothing else, else null. */
    private static ItemStack[] findPair(CraftingInput input) {
        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ManillaFolderItem)) return null;

            if (first.isEmpty()) {
                first = stack;
            } else if (second.isEmpty()) {
                second = stack;
            } else {
                return null; // three or more
            }
        }

        if (first.isEmpty() || second.isEmpty()) return null;

        return new ItemStack[] {first, second};
    }
}
