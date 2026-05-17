package net.bobofraggins.tremendousstorage.external.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * JEI recipe category for the Positive Vibes cauldron.
 *
 * <p>Displays a vertical guide showing four items top-to-bottom with downward arrows between
 * them. Handles two recipes: making Positive Vibes (Glistering Melon → Water Cauldron → Healing
 * Positive Vibe Cauldron → Bucket) and healing a Zombie Brain (Zombie Brain → Positive Vibes Bucket →
 * Cauldron → Brain).
 */
@SuppressWarnings("removal")
public class PositiveVibesCauldronCategory implements IRecipeCategory<PositiveVibesCauldronJeiRecipe> {

    public static final RecipeType<PositiveVibesCauldronJeiRecipe> RECIPE_TYPE =
            RecipeType.create("tremendousstorage", "positive_vibes_cauldron", PositiveVibesCauldronJeiRecipe.class);

    private static final int SLOT_X = 4;
    private static final int[] SLOT_YS = {0, 24, 48, 72};
    private static final int WIDTH = 26;
    private static final int HEIGHT = 90;
    private static final int ARROW_COLOR = 0xFF555555;

    private final IDrawable icon;

    public PositiveVibesCauldronCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(Registration.POSITIVE_VIBES_CAULDRON_ITEM.get());
    }

    @Override
    public RecipeType<PositiveVibesCauldronJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tremendousstorage.positive_vibes_cauldron");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PositiveVibesCauldronJeiRecipe recipe, IFocusGroup focuses) {
        var steps = recipe.steps();
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            builder.addSlot(step.role(), SLOT_X, SLOT_YS[i])
                    .addItemStack(step.stack())
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(step.tooltip()));
        }
    }

    @Override
    public void draw(
            PositiveVibesCauldronJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        int count = recipe.steps().size();
        for (int i = 1; i < count; i++) {
            drawDownArrow(guiGraphics, SLOT_YS[i] - 5);
        }
    }

    /**
     * Draws a small downward-pointing triangle (5×3 px) centred horizontally in the background,
     * with its top row at {@code y}.
     */
    private void drawDownArrow(GuiGraphicsExtractor g, int y) {
        int cx = WIDTH / 2;
        g.fill(cx - 2, y, cx + 3, y + 1, ARROW_COLOR);
        g.fill(cx - 1, y + 1, cx + 2, y + 2, ARROW_COLOR);
        g.fill(cx, y + 2, cx + 1, y + 3, ARROW_COLOR);
    }
}
