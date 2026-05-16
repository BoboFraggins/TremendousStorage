package net.bobofraggins.tremendousstorage.lazurite;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: any lazurite tool + one lazurite ingot → same tool fully repaired.
 *
 * <p>All data components (enchantments, custom name, etc.) are preserved; only
 * {@link DataComponents#DAMAGE} is reset to zero.
 */
public class LazuriteRepairRecipe extends CustomRecipe {

    public LazuriteRepairRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack tool = ItemStack.EMPTY;
        boolean hasIngot = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (isLazuriteTool(stack)) {
                if (!tool.isEmpty()) return false;
                tool = stack;
            } else if (stack.is(Registration.LAZURITE_INGOT.get())) {
                if (hasIngot) return false;
                hasIngot = true;
            } else {
                return false;
            }
        }

        return !tool.isEmpty() && hasIngot;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isLazuriteTool(stack)) {
                ItemStack repaired = stack.copy();
                repaired.set(DataComponents.DAMAGE, 0);
                return repaired;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return net.bobofraggins.tremendousstorage.shared.register.Registration.LAZURITE_REPAIR_RECIPE.get();
    }

    private static final java.util.Set<java.util.function.Supplier<? extends Item>> LAZURITE_TOOLS = java.util.Set.of(
            Registration.LAZURITE_PICKAXE,
            Registration.LAZURITE_AXE,
            Registration.LAZURITE_SHOVEL,
            Registration.LAZURITE_SWORD,
            Registration.LAZURITE_HOE,
            Registration.LAZURITE_PAXEL);

    private static boolean isLazuriteTool(ItemStack stack) {
        Item item = stack.getItem();
        for (var supplier : LAZURITE_TOOLS) {
            if (supplier.get() == item) return true;
        }
        return false;
    }
}
