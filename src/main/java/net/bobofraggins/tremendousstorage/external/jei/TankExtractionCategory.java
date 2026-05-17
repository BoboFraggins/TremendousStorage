package net.bobofraggins.tremendousstorage.external.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * JEI recipe category for right-click extractions from a Tank.
 *
 * <p>Layout: [Tank (catalyst)] [Container (input)]  →  [Result (output)]
 */
@SuppressWarnings("removal")
public class TankExtractionCategory implements IRecipeCategory<TankExtractionJeiRecipe> {

    public static final RecipeType<TankExtractionJeiRecipe> RECIPE_TYPE =
            RecipeType.create("tremendousstorage", "tank_extraction", TankExtractionJeiRecipe.class);

    private static final int TANK_X = 0;
    private static final int CONTAINER_X = 20;
    private static final int RESULT_X = 54;
    private static final int SLOT_Y = 1;
    private static final int WIDTH = 72;
    private static final int HEIGHT = 20;
    private static final int ARROW_COLOR = 0xFF555555;

    private final IDrawable icon;

    public TankExtractionCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(Registration.TANK_ITEM.get());
    }

    @Override
    public RecipeType<TankExtractionJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tremendousstorage.tank_extraction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, TankExtractionJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, TANK_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.tank());

        builder.addSlot(RecipeIngredientRole.INPUT, CONTAINER_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.container());

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.result());
    }

    @Override
    public void draw(
            TankExtractionJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        int cy = SLOT_Y + 8;
        int x = 40;
        guiGraphics.fill(x, cy, x + 10, cy + 1, ARROW_COLOR);
        guiGraphics.fill(x + 8, cy - 2, x + 9, cy + 3, ARROW_COLOR);
        guiGraphics.fill(x + 9, cy - 1, x + 10, cy + 2, ARROW_COLOR);
        guiGraphics.fill(x + 10, cy, x + 11, cy + 1, ARROW_COLOR);
    }
}
