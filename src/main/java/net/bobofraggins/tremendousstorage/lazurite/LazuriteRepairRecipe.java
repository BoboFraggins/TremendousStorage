package net.bobofraggins.tremendousstorage.lazurite;

import java.util.Map;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: any lazurite tool + one or more lazurite ingots → same tool partially repaired.
 *
 * <p>Each ingot repairs {@code floor(maxDurability / ingotsUsedToCraft)} durability, so the
 * number of ingots required to fully repair a tool matches the number used to craft it:
 *
 * <ul>
 *   <li>Shovel (1 ingot to craft) → 1 ingot repairs 100%
 *   <li>Sword / Hoe (2 ingots) → 1 ingot repairs 50%
 *   <li>Pickaxe / Axe (3 ingots) → 1 ingot repairs ~33%
 *   <li>Paxel (9 ingots equivalent) → 1 ingot repairs ~11%
 * </ul>
 *
 * <p>All data components (enchantments, custom name, etc.) are preserved. The recipe is only
 * accepted when the tool is actually damaged.
 */
public class LazuriteRepairRecipe extends CustomRecipe {

    public LazuriteRepairRecipe() {
        super();
    }

    // Lazily initialised after items are registered.
    private static volatile Map<Item, Integer> ingotCounts = null;

    /** Returns the number of Lazurite ingots used to craft each tool (the repair divisor). */
    private static Map<Item, Integer> ingotCounts() {
        if (ingotCounts == null) {
            ingotCounts = Map.of(
                    Registration.LAZURITE_SHOVEL.get(), 1,
                    Registration.LAZURITE_SWORD.get(), 2,
                    Registration.LAZURITE_HOE.get(), 2,
                    Registration.LAZURITE_PICKAXE.get(), 3,
                    Registration.LAZURITE_AXE.get(), 3,
                    Registration.LAZURITE_PAXEL.get(), 9);
        }
        return ingotCounts;
    }

    private static boolean isLazuriteTool(ItemStack stack) {
        return ingotCounts().containsKey(stack.getItem());
    }

    /** Ingots needed to fully repair this tool (= ingots used to craft it). */
    private static int ingotsToFullyRepair(ItemStack tool) {
        return ingotCounts().getOrDefault(tool.getItem(), 1);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack tool = ItemStack.EMPTY;
        int ingotCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (isLazuriteTool(stack)) {
                if (!tool.isEmpty()) return false; // only one tool allowed
                tool = stack;
            } else if (stack.is(Registration.LAZURITE_INGOT.get())) {
                ingotCount++;
            } else {
                return false; // unknown item in grid
            }
        }

        if (tool.isEmpty() || ingotCount == 0) return false;

        // Only accept the recipe if the tool is actually damaged
        int damage = tool.getOrDefault(DataComponents.DAMAGE, 0);
        return damage > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack tool = ItemStack.EMPTY;
        int ingotCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (isLazuriteTool(stack)) {
                tool = stack;
            } else if (stack.is(Registration.LAZURITE_INGOT.get())) {
                ingotCount++;
            }
        }

        if (tool.isEmpty() || ingotCount == 0) return ItemStack.EMPTY;

        int maxDamage = tool.getMaxDamage();
        int repairPerIngot = maxDamage / ingotsToFullyRepair(tool);
        int currentDamage = tool.getOrDefault(DataComponents.DAMAGE, 0);
        int newDamage = Math.max(0, currentDamage - ingotCount * repairPerIngot);

        ItemStack repaired = tool.copy();
        repaired.set(DataComponents.DAMAGE, newDamage);
        return repaired;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Registration.LAZURITE_REPAIR_RECIPE.get();
    }
}
