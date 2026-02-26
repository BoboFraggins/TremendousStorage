package net.bobofraggins.intellistore.manillafolder;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
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

    public static final MapCodec<FolderStorageRecipe> CODEC =
            MapCodec.unit(new FolderStorageRecipe(CraftingBookCategory.MISC));

    public static final StreamCodec<RegistryFriendlyByteBuf, FolderStorageRecipe> STREAM_CODEC =
            StreamCodec.unit(new FolderStorageRecipe(CraftingBookCategory.MISC));

    public FolderStorageRecipe(CraftingBookCategory category) {
        super(category);
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
        long capacity = ((ManillaFolderItem) folder.getItem()).getCapacity();

        // Folder is full — nothing to insert
        if (contents.count() >= capacity) return false;

        // Damageable items (tools, weapons, armour) are not supported
        if (item.isDamageableItem()) return false;

        // Unlocked folder accepts any item; locked folder must match
        return contents.accepts(item);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
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
        long capacity = ((ManillaFolderItem) folder.getItem()).getCapacity();

        // Lock the folder to this item type on first insert
        if (contents.isEmpty()) {
            contents = new FolderContents(java.util.Optional.of(item.copyWithCount(1)), 0L);
        }

        FolderContents.InsertResult result = contents.insert(item.getCount(), capacity);
        return ManillaFolderItem.setContents(folder.copyWithCount(1), result.updated());
    }

    /**
     * Returns any items that didn't fit into the folder back into the crafting grid slot they came
     * from. The folder slot gets the updated folder (via normal output). The item slot gets the
     * overflow remainder, or empty if everything fit.
     */
    @Override
    public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        net.minecraft.core.NonNullList<ItemStack> remaining =
                net.minecraft.core.NonNullList.withSize(input.size(), ItemStack.EMPTY);

        ItemStack folder = ItemStack.EMPTY;
        int itemSlot = -1;
        ItemStack item = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ManillaFolderItem) {
                folder = stack;
            } else {
                itemSlot = i;
                item = stack;
            }
        }

        if (folder.isEmpty() || item.isEmpty() || itemSlot < 0) return remaining;

        FolderContents contents = ManillaFolderItem.getContents(folder);
        long capacity = ((ManillaFolderItem) folder.getItem()).getCapacity();

        if (contents.isEmpty()) {
            contents = new FolderContents(java.util.Optional.of(item.copyWithCount(1)), 0L);
        }

        FolderContents.InsertResult result = contents.insert(item.getCount(), capacity);

        if (result.remainder() > 0) {
            remaining.set(itemSlot, item.copyWithCount((int) result.remainder()));
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return net.bobofraggins.intellistore.register.Registration.FOLDER_STORAGE_RECIPE.get();
    }
}
