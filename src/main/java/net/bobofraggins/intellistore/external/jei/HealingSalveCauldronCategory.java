package net.bobofraggins.intellistore.external.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * JEI recipe category for the Healing Salve cauldron.
 *
 * <p>Displays a vertical guide showing four items top-to-bottom with downward arrows between
 * them. Handles two recipes: making Healing Salve (Glistering Melon → Water Cauldron → Healing
 * Salve Cauldron → Bucket) and healing a Zombie Brain (Zombie Brain → Healing Salve Bucket →
 * Cauldron → Brain).
 */
public class HealingSalveCauldronCategory implements IRecipeCategory<HealingSalveCauldronJeiRecipe> {

    public static final RecipeType<HealingSalveCauldronJeiRecipe> RECIPE_TYPE =
            RecipeType.create("intellistore", "healing_salve_cauldron", HealingSalveCauldronJeiRecipe.class);

    private static final int SLOT_X = 4;
    private static final int[] SLOT_YS = {0, 24, 48, 72};
    private static final int WIDTH = 26;
    private static final int HEIGHT = 90;
    private static final int ARROW_COLOR = 0xFF555555;

    private final IDrawable icon;

    public HealingSalveCauldronCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(Registration.HEALING_SALVE_CAULDRON_ITEM.get());
    }

    @Override
    public RecipeType<HealingSalveCauldronJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.intellistore.healing_salve_cauldron");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HealingSalveCauldronJeiRecipe recipe, IFocusGroup focuses) {
        var steps = recipe.steps();
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            builder.addSlot(step.role(), SLOT_X, SLOT_YS[i])
                    .addItemStack(step.stack())
                    .addTooltipCallback((slotView, tooltip) -> tooltip.add(step.tooltip()));
        }
    }

    @Override
    public void draw(
            HealingSalveCauldronJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        drawDownArrow(guiGraphics, 19);
        drawDownArrow(guiGraphics, 43);
        drawDownArrow(guiGraphics, 67);
    }

    /**
     * Draws a small downward-pointing triangle (5×3 px) centred horizontally in the background,
     * with its top row at {@code y}.
     */
    private void drawDownArrow(GuiGraphics g, int y) {
        int cx = WIDTH / 2;
        g.fill(cx - 2, y, cx + 3, y + 1, ARROW_COLOR);
        g.fill(cx - 1, y + 1, cx + 2, y + 2, ARROW_COLOR);
        g.fill(cx, y + 2, cx + 1, y + 3, ARROW_COLOR);
    }
}
