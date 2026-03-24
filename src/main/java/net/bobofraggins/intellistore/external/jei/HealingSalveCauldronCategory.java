package net.bobofraggins.intellistore.external.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI recipe category for the Healing Salve cauldron: Zombie Brain → Brain.
 *
 * <p>Displays a vertical guide showing the four items involved: Zombie Brain → Healing Salve
 * Bucket → Cauldron → Brain, with small downward arrows between each step.
 */
public class HealingSalveCauldronCategory implements IRecipeCategory<HealingSalveCauldronJeiRecipe> {

    public static final RecipeType<HealingSalveCauldronJeiRecipe> RECIPE_TYPE =
            RecipeType.create("intellistore", "healing_salve_cauldron", HealingSalveCauldronJeiRecipe.class);

    private static final int SLOT_X = 4;
    private static final int SLOT_Y_0 = 0;
    private static final int SLOT_Y_1 = 24;
    private static final int SLOT_Y_2 = 48;
    private static final int SLOT_Y_3 = 72;
    private static final int WIDTH = 26;
    private static final int HEIGHT = 90;
    private static final int ARROW_COLOR = 0xFF555555;

    private final IDrawable background;
    private final IDrawable icon;

    public HealingSalveCauldronCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(WIDTH, HEIGHT);
        icon = helper.createDrawableItemLike(Items.CAULDRON);
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
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HealingSalveCauldronJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, SLOT_Y_0)
                .addItemStack(new ItemStack(Registration.ZOMBIE_BRAIN.get()));
        builder.addSlot(RecipeIngredientRole.CATALYST, SLOT_X, SLOT_Y_1)
                .addItemStack(new ItemStack(Registration.HEALING_SALVE_BUCKET.get()));
        builder.addSlot(RecipeIngredientRole.CATALYST, SLOT_X, SLOT_Y_2).addItemStack(new ItemStack(Items.CAULDRON));
        builder.addSlot(RecipeIngredientRole.OUTPUT, SLOT_X, SLOT_Y_3)
                .addItemStack(new ItemStack(Registration.BRAIN.get()));
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
