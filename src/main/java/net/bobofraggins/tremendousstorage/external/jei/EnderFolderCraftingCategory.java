package net.bobofraggins.tremendousstorage.external.jei;

import java.util.Arrays;
import java.util.List;
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
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe category for the Ender Folder crafting recipe.
 *
 * <p>Displays a two-input (folder + Ender Storage Upgrade), two-output (pair of linked
 * Ender Folders) layout to convey that both folders are produced together. Cycles through
 * all folder tiers.
 *
 * <p>Layout (slots are 16×16):
 * <pre>
 *   [folder]           →   [ender folder]
 *   [ender storage upgrade] [ender folder]
 * </pre>
 */
public class EnderFolderCraftingCategory implements IRecipeCategory<EnderFolderCraftingCategory.Recipe> {

    /**
     * Marker recipe object. There is only one logical recipe (all tiers cycle in the
     * ingredient slots), so a single static instance is sufficient.
     */
    public static final class Recipe {
        static final Recipe INSTANCE = new Recipe();

        private Recipe() {}
    }

    public static final RecipeType<Recipe> RECIPE_TYPE =
            RecipeType.create("tremendousstorage", "ender_folder_crafting", Recipe.class);

    // Layout constants
    private static final int INPUT_X = 0;
    private static final int OUTPUT_X = 42;
    private static final int ROW_0_Y = 0;
    private static final int ROW_1_Y = 22;
    private static final int WIDTH = 58;
    private static final int HEIGHT = 38;
    private static final int ARROW_COLOR = 0xFF555555;

    private final IDrawable icon;

    public EnderFolderCraftingCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(Registration.ENDER_STORAGE_UPGRADE.get());
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tremendousstorage.ender_folder_crafting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        StorageTier[] tiers = StorageTier.values();

        List<ItemStack> folders = Arrays.stream(tiers)
                .map(EnderFolderCraftingCategory::unlockedFolder)
                .toList();

        List<ItemStack> upgrades = Arrays.stream(tiers)
                .map(t -> new ItemStack(Registration.ENDER_STORAGE_UPGRADE.get()))
                .toList();

        List<ItemStack> outputs = Arrays.stream(tiers)
                .map(EnderFolderCraftingCategory::enderFolder)
                .toList();

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, ROW_0_Y).addIngredients(VanillaTypes.ITEM_STACK, folders);

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, ROW_1_Y).addIngredients(VanillaTypes.ITEM_STACK, upgrades);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, ROW_0_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, outputs);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, ROW_1_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, outputs);
    }

    @Override
    public void draw(
            Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Right-pointing arrow centred vertically between the two input rows
        drawRightArrow(guiGraphics, 19, HEIGHT / 2 - 1);
    }

    /**
     * Draws a small right-pointing arrow with its tip at {@code (x + 13, cy)}.
     */
    private void drawRightArrow(GuiGraphics g, int x, int cy) {
        g.fill(x, cy, x + 10, cy + 1, ARROW_COLOR); // shaft
        g.fill(x + 8, cy - 2, x + 9, cy + 3, ARROW_COLOR); // arrowhead col 1
        g.fill(x + 9, cy - 1, x + 10, cy + 2, ARROW_COLOR); // arrowhead col 2
        g.fill(x + 10, cy, x + 11, cy + 1, ARROW_COLOR); // tip
    }

    // -------------------------------------------------------------------------
    // Item stack helpers
    // -------------------------------------------------------------------------

    private static ItemStack unlockedFolder(StorageTier tier) {
        ItemStack folder = new ItemStack(Registration.MANILA_FOLDER.get());
        return ManillaFolderItem.setContents(folder, FolderContents.EMPTY.withTier(tier));
    }

    private static ItemStack enderFolder(StorageTier tier) {
        ItemStack ef = new ItemStack(Registration.ENDER_FOLDER.get());
        ef.set(Registration.FOLDER_CONTENTS.get(), FolderContents.EMPTY.withTier(tier));
        EnderFolderItem.setLinkId(ef, 0L);
        return ef;
    }
}
