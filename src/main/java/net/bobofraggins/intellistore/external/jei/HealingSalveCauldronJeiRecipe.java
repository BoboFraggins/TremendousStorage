package net.bobofraggins.intellistore.external.jei;

import java.util.List;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Data carrier for a Healing Salve cauldron recipe guide displayed in JEI.
 *
 * <p>Each instance holds four {@link Step}s shown top-to-bottom in
 * {@link HealingSalveCauldronCategory}. Instances are created lazily (in
 * {@link #makeSalve()} / {@link #healBrain()}) so that item registry objects are
 * guaranteed to be populated by the time JEI calls {@code registerRecipes}.
 */
public final class HealingSalveCauldronJeiRecipe {

    /** One row in the vertical guide: the item to display, its JEI role, and a tooltip hint. */
    public record Step(ItemStack stack, RecipeIngredientRole role, Component tooltip) {}

    private final List<Step> steps;

    private HealingSalveCauldronJeiRecipe(List<Step> steps) {
        this.steps = steps;
    }

    public List<Step> steps() {
        return steps;
    }

    /** Making Healing Salve: Glistering Melon → Water Bucket → Cauldron → Healing Salve Bucket. */
    public static HealingSalveCauldronJeiRecipe makeSalve() {
        return new HealingSalveCauldronJeiRecipe(List.of(
                new Step(
                        new ItemStack(Items.GLISTERING_MELON_SLICE),
                        RecipeIngredientRole.INPUT,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.make.glistering_melon")),
                new Step(
                        new ItemStack(Items.WATER_BUCKET),
                        RecipeIngredientRole.CATALYST,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.make.water_bucket")),
                new Step(
                        new ItemStack(Items.CAULDRON),
                        RecipeIngredientRole.CATALYST,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.make.cauldron")),
                new Step(
                        new ItemStack(Registration.HEALING_SALVE_BUCKET.get()),
                        RecipeIngredientRole.OUTPUT,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.make.output"))));
    }

    /** Healing Zombie Brain: Zombie Brain → Healing Salve Bucket → Cauldron → Brain. */
    public static HealingSalveCauldronJeiRecipe healBrain() {
        return new HealingSalveCauldronJeiRecipe(List.of(
                new Step(
                        new ItemStack(Registration.ZOMBIE_BRAIN.get()),
                        RecipeIngredientRole.INPUT,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.heal.zombie_brain")),
                new Step(
                        new ItemStack(Registration.HEALING_SALVE_BUCKET.get()),
                        RecipeIngredientRole.CATALYST,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.heal.salve_bucket")),
                new Step(
                        new ItemStack(Items.CAULDRON),
                        RecipeIngredientRole.CATALYST,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.heal.cauldron")),
                new Step(
                        new ItemStack(Registration.BRAIN.get()),
                        RecipeIngredientRole.OUTPUT,
                        Component.translatable("jei.intellistore.healing_salve_cauldron.step.heal.output"))));
    }
}
