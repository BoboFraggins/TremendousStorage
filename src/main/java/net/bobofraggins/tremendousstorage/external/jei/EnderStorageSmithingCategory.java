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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * JEI recipe category for the Ender Chest and Ender Backpack smithing recipes.
 *
 * <p>Each {@link Recipe} carries a display title and a pair of per-tier item lists so
 * the single category can represent both the chest and backpack recipes, with cycling
 * through all tiers.
 *
 * <p>Layout (slots are 16×16):
 * <pre>
 *   [input item]        →   [ender output]
 *   [ender upgrade]         [ender output]
 * </pre>
 */
public class EnderStorageSmithingCategory
        implements IRecipeCategory<EnderStorageSmithingCategory.Recipe> {

    // -------------------------------------------------------------------------
    // Marker recipe — one entry per item type (chest / backpack)
    // -------------------------------------------------------------------------

    public static final class Recipe {

        public static final Recipe CHEST = new Recipe(
                "jei.tremendousstorage.ender_chest_smithing",
                tieredChests(),
                tieredEnderChests());

        public static final Recipe BACKPACK = new Recipe(
                "jei.tremendousstorage.ender_backpack_smithing",
                tieredBackpacks(),
                tieredEnderBackpacks());

        final String titleKey;
        final List<ItemStack> inputs;
        final List<ItemStack> outputs;

        private Recipe(String titleKey, List<ItemStack> inputs, List<ItemStack> outputs) {
            this.titleKey = titleKey;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        // -----------------------------------------------------------------------
        // Item stack helpers
        // -----------------------------------------------------------------------

        private static List<ItemStack> tieredChests() {
            return Arrays.stream(StorageTier.values())
                    .map(tier -> {
                        ItemStack stack = new ItemStack(Registration.TREMENDOUS_CHEST_ITEM.get());
                        CompoundTag tag = new CompoundTag();
                        tag.putString("Tier", tier.getId());
                        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                        return stack;
                    })
                    .toList();
        }

        private static List<ItemStack> tieredEnderChests() {
            return Arrays.stream(StorageTier.values())
                    .map(tier -> {
                        ItemStack stack = new ItemStack(Registration.ENDER_TREMENDOUS_CHEST_ITEM.get());
                        CompoundTag tag = new CompoundTag();
                        tag.putString("Tier", tier.getId());
                        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                        return stack;
                    })
                    .toList();
        }

        private static List<ItemStack> tieredBackpacks() {
            return Arrays.stream(StorageTier.values())
                    .map(tier -> new ItemStack(Registration.TREMENDOUS_BACKPACK.get()))
                    .toList();
        }

        private static List<ItemStack> tieredEnderBackpacks() {
            return Arrays.stream(StorageTier.values())
                    .map(tier -> new ItemStack(Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get()))
                    .toList();
        }
    }

    // -------------------------------------------------------------------------
    // Category metadata
    // -------------------------------------------------------------------------

    public static final RecipeType<Recipe> RECIPE_TYPE =
            RecipeType.create("tremendousstorage", "ender_storage_smithing", Recipe.class);

    private static final int INPUT_X = 0;
    private static final int OUTPUT_X = 42;
    private static final int ROW_0_Y = 0;
    private static final int ROW_1_Y = 22;
    private static final int WIDTH = 58;
    private static final int HEIGHT = 38;
    private static final int ARROW_COLOR = 0xFF555555;

    private final IDrawable icon;

    public EnderStorageSmithingCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(Registration.ENDER_STORAGE_UPGRADE.get());
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tremendousstorage.ender_storage_smithing");
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
        List<ItemStack> upgrades = Arrays.stream(StorageTier.values())
                .map(t -> new ItemStack(Registration.ENDER_STORAGE_UPGRADE.get()))
                .toList();

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, ROW_0_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.inputs);

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, ROW_1_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, upgrades);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, ROW_0_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.outputs);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, ROW_1_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.outputs);
    }

    @Override
    public void draw(
            Recipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        drawRightArrow(guiGraphics, 19, HEIGHT / 2 - 1);
    }

    private void drawRightArrow(GuiGraphics g, int x, int cy) {
        g.fill(x, cy, x + 10, cy + 1, ARROW_COLOR);
        g.fill(x + 8, cy - 2, x + 9, cy + 3, ARROW_COLOR);
        g.fill(x + 9, cy - 1, x + 10, cy + 2, ARROW_COLOR);
        g.fill(x + 10, cy, x + 11, cy + 1, ARROW_COLOR);
    }
}
