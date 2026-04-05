package net.bobofraggins.intellistore.storage.manillafolder;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.storageupgrade.StorageUpgradeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: Manila Folder (any tier) + matching Storage Upgrade → next-tier Manila Folder
 * with contents preserved.
 *
 * <p>The upgrade item's {@code fromTier} ID must match the folder's {@link FolderTier} ID (both
 * use the same name strings: "wood", "copper", etc.). The resulting folder is the next tier up,
 * carrying the exact same {@link FolderContents} component — item lock and count are untouched,
 * since upgrading only increases capacity.
 */
public class FolderUpgradeRecipe extends CustomRecipe {

    public FolderUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findPair(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] pair = findPair(input);
        if (pair == null) return ItemStack.EMPTY;

        ItemStack folderStack = pair[0];
        StorageUpgradeItem upgradeItem = (StorageUpgradeItem) pair[1].getItem();

        // Find the FolderTier corresponding to the upgrade's toTier by ID
        String toId = upgradeItem.getToTier().getId();
        FolderTier toFolderTier = null;
        for (FolderTier ft : FolderTier.values()) {
            if (ft.getId().equals(toId)) {
                toFolderTier = ft;
                break;
            }
        }
        if (toFolderTier == null) return ItemStack.EMPTY;

        ManillaFolderItem targetItem =
                Registration.MANILA_FOLDERS.get(toFolderTier).get();
        ItemStack result = new ItemStack(targetItem);
        return ManillaFolderItem.setContents(result, ManillaFolderItem.getContents(folderStack));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.FOLDER_UPGRADE_RECIPE.get();
    }

    /**
     * Returns [folderStack, upgradeStack] if the grid contains exactly one Manila Folder and one
     * Storage Upgrade whose {@code fromTier} matches the folder's tier, otherwise {@code null}.
     */
    private static ItemStack[] findPair(CraftingInput input) {
        ItemStack folder = ItemStack.EMPTY;
        ItemStack upgrade = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ManillaFolderItem) {
                if (!folder.isEmpty()) return null;
                folder = stack;
            } else if (stack.getItem() instanceof StorageUpgradeItem) {
                if (!upgrade.isEmpty()) return null;
                upgrade = stack;
            } else {
                return null;
            }
        }

        if (folder.isEmpty() || upgrade.isEmpty()) return null;

        ManillaFolderItem folderItem = (ManillaFolderItem) folder.getItem();
        StorageUpgradeItem upgradeItem = (StorageUpgradeItem) upgrade.getItem();
        if (!folderItem.getTier().getId().equals(upgradeItem.getFromTier().getId())) return null;

        return new ItemStack[] {folder, upgrade};
    }
}
