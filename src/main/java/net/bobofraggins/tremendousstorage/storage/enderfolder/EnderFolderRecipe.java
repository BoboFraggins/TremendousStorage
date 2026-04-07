package net.bobofraggins.tremendousstorage.storage.enderfolder;

import java.security.SecureRandom;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: 2 Manila Folders of the same tier + 1 Eye of Ender → 2 linked Ender Folders.
 *
 * <p>The two output Ender Folders share a freshly generated 64-bit {@code linkId}. Any two items
 * with the same link ID share the same live inventory via {@link EnderFolderStorage}.
 *
 * <p>The second output item is returned via {@link #getRemainingItems(CraftingInput)} in place of
 * one of the folder inputs. The primary output from {@link #assemble} is the first Ender Folder.
 */
public class EnderFolderRecipe extends CustomRecipe {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Carries the link ID generated in {@link #assemble} so that {@link #getRemainingItems}
     * can attach the same ID to the second output folder without regenerating it.
     *
     * <p>This is safe because crafting calls {@code assemble} then {@code getRemainingItems}
     * on the same thread without interleaving.
     */
    private static final ThreadLocal<long[]> PENDING_LINK = ThreadLocal.withInitial(() -> new long[] {-1L});

    public EnderFolderRecipe(CraftingBookCategory category) {
        super(category);
    }

    // -------------------------------------------------------------------------
    // Recipe matching
    // -------------------------------------------------------------------------

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findIngredients(input) != null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    // -------------------------------------------------------------------------
    // Assembly — primary output (first Ender Folder)
    // -------------------------------------------------------------------------

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Ingredients found = findIngredients(input);
        if (found == null) return ItemStack.EMPTY;

        long linkId = SECURE_RANDOM.nextLong();
        PENDING_LINK.get()[0] = linkId;
        return makeEnderFolder(found.folderContents(), linkId);
    }

    // -------------------------------------------------------------------------
    // Remaining items — second Ender Folder replaces one of the folder inputs
    // -------------------------------------------------------------------------

    /**
     * Returns the second linked Ender Folder in the slot that held the second Manila Folder
     * input. All other slots return {@link ItemStack#EMPTY} (consuming the Eye of Ender and
     * first folder normally).
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        long linkId = PENDING_LINK.get()[0];
        if (linkId == -1L) return remaining;
        PENDING_LINK.get()[0] = -1L; // consume

        Ingredients found = findIngredients(input);
        if (found == null) return remaining;

        remaining.set(found.secondFolderSlot(), makeEnderFolder(found.folderContents(), linkId));
        return remaining;
    }

    // -------------------------------------------------------------------------
    // Serializer
    // -------------------------------------------------------------------------

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ENDER_FOLDER_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ItemStack makeEnderFolder(FolderContents contents, long linkId) {
        ItemStack result = new ItemStack(Registration.ENDER_FOLDER.get());
        result.set(Registration.FOLDER_CONTENTS.get(), contents);
        EnderFolderItem.setLinkId(result, linkId);
        return result;
    }

    /**
     * Validates the grid and extracts the positions and contents of the two folder inputs + Eye of Ender.
     * Returns {@code null} if the grid does not match the recipe.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Exactly 2 Manila Folder items (same tier, not Ender Folders)</li>
     *   <li>Exactly 1 Eye of Ender</li>
     *   <li>No other items</li>
     * </ul>
     */
    private static Ingredients findIngredients(CraftingInput input) {
        int firstFolderSlot = -1;
        int secondFolderSlot = -1;
        boolean hasEye = false;
        FolderContents firstContents = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(Items.ENDER_EYE)) {
                if (hasEye) return null;
                hasEye = true;
            } else if (stack.getItem() instanceof ManillaFolderItem && !(stack.getItem() instanceof EnderFolderItem)) {
                FolderContents contents = ManillaFolderItem.getContents(stack);
                if (firstFolderSlot == -1) {
                    firstFolderSlot = i;
                    firstContents = contents;
                } else if (secondFolderSlot == -1) {
                    if (contents.tier() != firstContents.tier()) return null;
                    secondFolderSlot = i;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        if (!hasEye || firstFolderSlot == -1 || secondFolderSlot == -1) return null;
        return new Ingredients(firstContents, firstFolderSlot, secondFolderSlot);
    }

    private record Ingredients(FolderContents folderContents, int firstFolderSlot, int secondFolderSlot) {}
}
