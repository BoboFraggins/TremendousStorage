package net.bobofraggins.intellistore.storage.manillafolder;

import com.mojang.serialization.MapCodec;
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
 * Crafting grid recipe: locked Manila Folder (alone) → up to one full stack extracted per craft.
 *
 * <p>Behaviour:
 * <ul>
 *   <li><b>Click-drag</b>: takes up to one full stack from the output slot (capped at the stored
 *       item's max stack size and the folder's remaining count). The folder is returned to the grid
 *       with its count decremented by the amount extracted. The folder remains locked to its item
 *       type even if count reaches 0.
 *   <li><b>Shift-click</b>: Minecraft repeatedly calls {@code assemble} + {@code getRemainingItems}
 *       until the recipe no longer matches (count = 0) or the player's inventory is full, moving
 *       one stack per iteration to the inventory.
 * </ul>
 *
 * <p>Exactly one non-empty slot is required and it must be a locked Manila Folder with count > 0.
 *
 * <p>Edge cases:
 * <ul>
 *   <li><b>Folder drained to 0</b>: the folder stays locked to its item type with count 0. The
 *       recipe stops matching, so the output slot clears. The folder can be refilled later via
 *       {@link FolderStorageRecipe}.
 *   <li><b>Fewer items than a full stack</b>: e.g. folder has 10 stones — output is 10, folder
 *       returns with count 0 and stays locked to stone.
 *   <li><b>Unstackable items</b> (max stack size = 1, e.g. swords, bows): each craft yields
 *       exactly 1. Shift-click drains one per iteration.
 *   <li><b>Partially stackable items</b> (e.g. ender pearls, max 16): each craft yields up to 16.
 *       Shift-click moves 16 per iteration.
 *   <li><b>Folder locked but empty (count = 0)</b>: recipe does not match — no output.
 * </ul>
 */
public class FolderExtractRecipe extends CustomRecipe {

    public static final MapCodec<FolderExtractRecipe> CODEC =
            MapCodec.unit(new FolderExtractRecipe(CraftingBookCategory.MISC));

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderExtractRecipe> STREAM_CODEC =
            StreamCodec.unit(new FolderExtractRecipe(CraftingBookCategory.MISC));

    public FolderExtractRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack folder = findFolder(input);
        if (folder.isEmpty()) return false;
        FolderContents contents = ManillaFolderItem.getContents(folder);
        return !contents.isEmpty() && contents.count() > 0;
    }

    /** Extracts up to one full stack (capped at the stored item's max stack size). */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack folder = findFolder(input);
        if (folder.isEmpty()) return ItemStack.EMPTY;

        FolderContents contents = ManillaFolderItem.getContents(folder);
        if (contents.isEmpty() || contents.count() == 0) return ItemStack.EMPTY;

        ItemStack stored = contents.storedItem().get();
        int extractCount = (int) Math.min(stored.getMaxStackSize(), contents.count());
        return stored.copyWithCount(extractCount);
    }

    /**
     * Returns the folder to the crafting grid with count decremented by the extracted amount. The
     * folder remains locked to its item type even when count reaches 0.
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ManillaFolderItem)) continue;

            FolderContents contents = ManillaFolderItem.getContents(stack);
            ItemStack stored = contents.storedItem().get();
            long extractCount = Math.min(stored.getMaxStackSize(), contents.count());
            FolderContents.ExtractResult result = contents.extract(extractCount);

            // Folder stays locked to its item type — do not reset to EMPTY
            remaining.set(i, ManillaFolderItem.setContents(stack.copyWithCount(1), result.updated()));
            break;
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return net.bobofraggins.intellistore.shared.register.Registration.FOLDER_EXTRACT_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ItemStack findFolder(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ManillaFolderItem)) return ItemStack.EMPTY;
            if (!found.isEmpty()) return ItemStack.EMPTY; // more than one item
            found = stack;
        }
        return found;
    }
}
