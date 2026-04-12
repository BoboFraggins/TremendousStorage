package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/**
 * Registers Vex Repellent brewing stand recipes:
 * <ol>
 *   <li>Positive Vibes Bottle + Shield → Vex Repellent (1:00)
 *   <li>Vex Repellent (1:00) + Redstone → Extended Vex Repellent (3:00)
 *   <li>Extended Vex Repellent (3:00) + Redstone → Long Vex Repellent (8:00)
 * </ol>
 */
public final class VexRepellentBrewingRecipes {

    private VexRepellentBrewingRecipes() {}

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();

        builder.addRecipe(
                Ingredient.of(Registration.POSITIVE_VIBES_BOTTLE.get()),
                Ingredient.of(Items.SHIELD),
                new ItemStack(Registration.VEX_REPELLENT_POTION.get()));

        builder.addRecipe(
                Ingredient.of(Registration.VEX_REPELLENT_POTION.get()),
                Ingredient.of(Items.REDSTONE),
                new ItemStack(Registration.VEX_REPELLENT_POTION_EXTENDED.get()));

        builder.addRecipe(
                Ingredient.of(Registration.VEX_REPELLENT_POTION_EXTENDED.get()),
                Ingredient.of(Items.REDSTONE),
                new ItemStack(Registration.VEX_REPELLENT_POTION_LONG.get()));
    }
}
