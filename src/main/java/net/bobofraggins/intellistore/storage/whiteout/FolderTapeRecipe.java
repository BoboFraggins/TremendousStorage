package net.bobofraggins.intellistore.storage.whiteout;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankContents;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: Whiteout Tape + a locked-but-empty Manila Folder or Fluid Tank → unlocked item.
 *
 * <p>The tape loses one durability per use via {@link #getRemainingItems}. When the tape
 * would exceed its max damage it is fully consumed (not returned as a broken item).
 *
 * <p>The recipe requires exactly two non-empty slots: one tape and one lockable item that is
 * locked (has a stored type) but has a count/amount of zero.
 */
public class FolderTapeRecipe extends CustomRecipe {

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

    private static ItemStack findTankItem(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.has(Registration.FLUID_TANK_CONTENTS.get())) return s;
        }
        return ItemStack.EMPTY;
    }

    /** True if the folder is locked (has a stored item type) but currently empty (count == 0). */
    private static boolean isFolderLockedEmpty(ItemStack folder) {
        FolderContents contents = ManillaFolderItem.getContents(folder);
        return contents.storedItem().isPresent() && contents.count() == 0;
    }

    /** True if the tank item is locked to a fluid type but has amount == 0. */
    private static boolean isTankLockedEmpty(ItemStack tank) {
        FluidTankContents contents = tank.getOrDefault(Registration.FLUID_TANK_CONTENTS.get(), FluidTankContents.EMPTY);
        return contents.isLocked() && contents.amount() == 0;
    }

    // -------------------------------------------------------------------------
    // CustomRecipe
    // -------------------------------------------------------------------------

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack tape = ItemStack.EMPTY;
        ItemStack target = ItemStack.EMPTY; // folder or tank

        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;

            if (s.getItem() instanceof WhiteoutTapeItem) {
                if (!tape.isEmpty()) return false; // two tapes
                tape = s;
            } else if (s.getItem() instanceof ManillaFolderItem || s.has(Registration.FLUID_TANK_CONTENTS.get())) {
                if (!target.isEmpty()) return false; // two targets
                target = s;
            } else {
                return false; // unexpected item
            }
        }

        if (tape.isEmpty() || target.isEmpty()) return false;

        if (target.getItem() instanceof ManillaFolderItem) {
            return isFolderLockedEmpty(target);
        } else {
            return isTankLockedEmpty(target);
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack folder = findFolder(input);
        if (!folder.isEmpty()) {
            return ManillaFolderItem.setContents(folder.copyWithCount(1), FolderContents.EMPTY);
        }

        ItemStack tank = findTankItem(input);
        if (!tank.isEmpty()) {
            ItemStack result = tank.copyWithCount(1);
            result.set(Registration.FLUID_TANK_CONTENTS.get(), FluidTankContents.EMPTY);
            return result;
        }

        return ItemStack.EMPTY;
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
