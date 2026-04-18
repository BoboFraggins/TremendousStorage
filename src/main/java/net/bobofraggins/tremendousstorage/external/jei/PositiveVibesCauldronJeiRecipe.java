package net.bobofraggins.tremendousstorage.external.jei;

import java.util.List;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Data carrier for a Positive Vibes cauldron recipe guide displayed in JEI.
 *
 * <p>Each instance holds four {@link Step}s shown top-to-bottom in
 * {@link PositiveVibesCauldronCategory}. Instances are created lazily (in
 * {@link #makeSalve()} / {@link #healBrain()}) so that item registry objects are
 * guaranteed to be populated by the time JEI calls {@code registerRecipes}.
 */
public final class PositiveVibesCauldronJeiRecipe {

    /** One row in the vertical guide: the item to display, its JEI role, and a tooltip hint. */
    public record Step(ItemStack stack, RecipeIngredientRole role, Component tooltip) {}

    private final List<Step> steps;

    private PositiveVibesCauldronJeiRecipe(List<Step> steps) {
        this.steps = steps;
    }

    public List<Step> steps() {
        return steps;
    }

    /** Positive Vibes: obtained by recycling items in the Recycling Bin. */
    public static PositiveVibesCauldronJeiRecipe obtainedByRecycling() {
        return new PositiveVibesCauldronJeiRecipe(List.of(new Step(
                new ItemStack(Registration.POSITIVE_VIBES_BUCKET.get()),
                RecipeIngredientRole.OUTPUT,
                Component.translatable("jei.tremendousstorage.positive_vibes.obtained_by_recycling"))));
    }

    /** Healing Zombie Brain: Zombie Brain → Positive Vibes Bucket → Cauldron → Brain. */
    public static PositiveVibesCauldronJeiRecipe healBrain() {
        return new PositiveVibesCauldronJeiRecipe(List.of(
                new Step(
                        new ItemStack(Registration.ZOMBIE_BRAIN.get()),
                        RecipeIngredientRole.INPUT,
                        Component.translatable("jei.tremendousstorage.positive_vibes_cauldron.step.heal.zombie_brain")),
                new Step(
                        new ItemStack(Registration.POSITIVE_VIBES_BUCKET.get()),
                        RecipeIngredientRole.CATALYST,
                        Component.translatable("jei.tremendousstorage.positive_vibes_cauldron.step.heal.salve_bucket")),
                new Step(
                        new ItemStack(Items.CAULDRON),
                        RecipeIngredientRole.CATALYST,
                        Component.translatable("jei.tremendousstorage.positive_vibes_cauldron.step.heal.cauldron")),
                new Step(
                        new ItemStack(Registration.BRAIN.get()),
                        RecipeIngredientRole.OUTPUT,
                        Component.translatable("jei.tremendousstorage.positive_vibes_cauldron.step.heal.output"))));
    }
}
