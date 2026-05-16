package net.bobofraggins.tremendousstorage.storage.manillafolder;

import net.bobofraggins.tremendousstorage.storage.baseupgrade.BaseUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.CraftingUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.HaarpUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.InterdimensionalUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.MagnetUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.PullerUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.storageupgrade.StorageUpgradeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting grid recipe: Manila Folder + matching item stack → folder with items inserted.
 *
 * <p>Two cases are handled:
 * <ol>
 *   <li>Unlocked folder + any non-folder item → folder locked to that item, all stack items
 *       inserted up to capacity.
 *   <li>Locked folder + matching item stack → folder with additional items inserted up to
 *       capacity. Any items that do not fit are returned via the container item mechanism.
 * </ol>
 *
 * <p>Exactly two non-empty slots are required: one Manila Folder and one item stack. Damageable
 * items (tools, weapons, armour) are rejected.
 */
public class FolderStorageRecipe extends CustomRecipe {

    public FolderStorageRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack folder = ItemStack.EMPTY;
        ItemStack item = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ManillaFolderItem) {
                if (!folder.isEmpty()) return false; // two folders
                folder = stack;
            } else {
                if (!item.isEmpty()) return false; // two non-folder items
                item = stack;
            }
        }

        if (folder.isEmpty() || item.isEmpty()) return false;

        FolderContents contents = ManillaFolderItem.getContents(folder);
        long capacity = ManillaFolderItem.getCapacity(folder);

        // Folder is full — nothing to insert
        if (contents.count() >= capacity) return false;

        // Damageable items (tools, weapons, armour) are not supported
        if (item.isDamageableItem()) return false;

        // Upgrade items should apply to the folder, not be stored inside it
        if (isUpgradeItem(item)) return false;

        // Unlocked folder accepts any item; locked folder must match
        return contents.accepts(item);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack folder = ItemStack.EMPTY;
        ItemStack item = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ManillaFolderItem) {
                folder = stack;
            } else {
                item = stack;
            }
        }

        if (folder.isEmpty() || item.isEmpty()) return ItemStack.EMPTY;

        FolderContents contents = ManillaFolderItem.getContents(folder);
        long capacity = ManillaFolderItem.getCapacity(folder);

        // Lock the folder to this item type on first insert
        if (contents.isEmpty()) {
            contents = new FolderContents(
                    java.util.Optional.of(item.copyWithCount(1)),
                    0L,
                    ManillaFolderItem.getContents(folder).tier());
        }

        FolderContents.InsertResult result = contents.insert(item.getCount(), capacity);
        return ManillaFolderItem.setContents(folder.copyWithCount(1), result.updated());
    }

    /**
     * Vanilla removes exactly 1 from each input slot. Since assemble() also inserts exactly 1,
     * nothing needs to be put back — both inputs are fully consumed per craft.
     * Shift-clicking repeatedly crafts until the item slot is empty or the folder is full.
     */
    @Override
    public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return net.minecraft.core.NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    private static boolean isUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof StorageUpgradeItem
                || stack.getItem() instanceof BaseUpgradeItem
                || stack.getItem() instanceof CraftingUpgradeItem
                || stack.getItem() instanceof MagnetUpgradeItem
                || stack.getItem() instanceof HaarpUpgradeItem
                || stack.getItem() instanceof PullerUpgradeItem
                || stack.getItem() instanceof InterdimensionalUpgradeItem;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.FOLDER_STORAGE_RECIPE.get();
    }
}
