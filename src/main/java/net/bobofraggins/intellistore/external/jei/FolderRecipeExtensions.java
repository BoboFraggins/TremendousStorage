package net.bobofraggins.intellistore.external.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.bobofraggins.intellistore.storage.manillafolder.FolderExtractRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.FolderMergeRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.FolderStorageRecipe;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.intellistore.storage.whiteout.FolderTapeRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI {@link ICraftingCategoryExtension} implementations for IntelliStore's custom crafting
 * recipes. Each inner class handles one recipe type and generates representative example
 * grids — one per folder tier — that JEI cycles through.
 */
public final class FolderRecipeExtensions {

    private FolderRecipeExtensions() {}

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Example item used to fill folders in the display (Diamond — easy to recognise). */
    private static ItemStack exampleItem() {
        return new ItemStack(Items.DIAMOND);
    }

    /** A folder of the given tier filled with {@code count} diamonds. */
    private static ItemStack filledFolder(StorageTier tier, long count) {
        ItemStack folder = new ItemStack(Registration.MANILA_FOLDER.get());
        FolderContents contents = new FolderContents(Optional.of(exampleItem().copyWithCount(1)), count, tier);
        return ManillaFolderItem.setContents(folder, contents);
    }

    /** An empty-but-locked folder of the given tier (count = 0, type = Diamond). */
    private static ItemStack lockedEmptyFolder(StorageTier tier) {
        return filledFolder(tier, 0L);
    }

    /** An unlocked (completely empty) folder of the given tier. */
    private static ItemStack unlockedFolder(StorageTier tier) {
        ItemStack folder = new ItemStack(Registration.MANILA_FOLDER.get());
        return ManillaFolderItem.setContents(folder, FolderContents.EMPTY.withTier(tier));
    }

    /** An Ender Folder of the given tier with a fixed demo link ID. */
    private static ItemStack enderFolder(StorageTier tier) {
        ItemStack ef = new ItemStack(Registration.ENDER_FOLDER.get());
        ef.set(Registration.FOLDER_CONTENTS.get(), FolderContents.EMPTY.withTier(tier));
        EnderFolderItem.setLinkId(ef, 0L);
        return ef;
    }

    // -------------------------------------------------------------------------
    // FolderStorageRecipe — folder + item → folder with items inserted
    // -------------------------------------------------------------------------

    /**
     * Shows one example per tier: {@code [empty folder] + [diamond]} → {@code [folder with diamond]}.
     * Cycles through all tiers in JEI.
     */
    static final class StorageExtension implements ICraftingCategoryExtension<FolderStorageRecipe> {

        private static final StorageTier[] TIERS = StorageTier.values();

        @Override
        public void setRecipe(
                RecipeHolder<FolderStorageRecipe> recipeHolder,
                IRecipeLayoutBuilder builder,
                ICraftingGridHelper helper,
                IFocusGroup focuses) {
            // Build one pair of inputs per tier
            List<List<ItemStack>> inputs = new ArrayList<>();
            List<ItemStack> foldersForSlot0 = new ArrayList<>();
            List<ItemStack> itemsForSlot1 = new ArrayList<>();

            for (StorageTier tier : TIERS) {
                foldersForSlot0.add(unlockedFolder(tier));
                itemsForSlot1.add(exampleItem());
            }
            inputs.add(foldersForSlot0);
            inputs.add(itemsForSlot1);

            // Fill remaining 7 slots with empty lists
            for (int i = 2; i < 9; i++) {
                inputs.add(List.of());
            }

            helper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 1, 1);

            // Output: folder filled with diamonds (use first tier as representative)
            helper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, Arrays.asList(filledFolder(TIERS[0], 1)));
        }
    }

    // -------------------------------------------------------------------------
    // FolderExtractRecipe — locked folder alone → output items + folder back
    // -------------------------------------------------------------------------

    /** Shows: {@code [folder with diamonds]} → {@code [diamonds]}, cycling through tiers. */
    static final class ExtractExtension implements ICraftingCategoryExtension<FolderExtractRecipe> {

        private static final StorageTier[] TIERS = StorageTier.values();

        @Override
        public void setRecipe(
                RecipeHolder<FolderExtractRecipe> recipeHolder,
                IRecipeLayoutBuilder builder,
                ICraftingGridHelper helper,
                IFocusGroup focuses) {
            List<List<ItemStack>> inputs = new ArrayList<>();

            List<ItemStack> foldersForSlot0 = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                foldersForSlot0.add(filledFolder(tier, 64L));
            }
            inputs.add(foldersForSlot0);

            for (int i = 1; i < 9; i++) {
                inputs.add(List.of());
            }

            helper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 1, 1);
            helper.createAndSetOutputs(
                    builder, VanillaTypes.ITEM_STACK, List.of(exampleItem().copyWithCount(64)));
        }
    }

    // -------------------------------------------------------------------------
    // FolderMergeRecipe — two same-tier filled folders → one combined folder
    // -------------------------------------------------------------------------

    /** Shows: {@code [folder A] + [folder B]} → {@code [merged folder]}, cycling through tiers. */
    static final class MergeExtension implements ICraftingCategoryExtension<FolderMergeRecipe> {

        private static final StorageTier[] TIERS = StorageTier.values();

        @Override
        public void setRecipe(
                RecipeHolder<FolderMergeRecipe> recipeHolder,
                IRecipeLayoutBuilder builder,
                ICraftingGridHelper helper,
                IFocusGroup focuses) {
            List<List<ItemStack>> inputs = new ArrayList<>();

            List<ItemStack> slot0 = new ArrayList<>();
            List<ItemStack> slot1 = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                slot0.add(filledFolder(tier, 32L));
                slot1.add(filledFolder(tier, 32L));
            }
            inputs.add(slot0);
            inputs.add(slot1);

            for (int i = 2; i < 9; i++) {
                inputs.add(List.of());
            }

            helper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 1, 1);

            List<ItemStack> outputs = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                outputs.add(filledFolder(tier, 64L));
            }
            helper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, outputs);
        }
    }

    // -------------------------------------------------------------------------
    // FolderTapeRecipe — whiteout tape + locked-empty folder → unlocked folder
    // -------------------------------------------------------------------------

    /** Shows: {@code [whiteout tape] + [locked-empty folder]} → {@code [unlocked folder]}. */
    static final class TapeExtension implements ICraftingCategoryExtension<FolderTapeRecipe> {

        private static final StorageTier[] TIERS = StorageTier.values();

        @Override
        public void setRecipe(
                RecipeHolder<FolderTapeRecipe> recipeHolder,
                IRecipeLayoutBuilder builder,
                ICraftingGridHelper helper,
                IFocusGroup focuses) {
            List<List<ItemStack>> inputs = new ArrayList<>();

            List<ItemStack> tapes = new ArrayList<>();
            List<ItemStack> folders = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                tapes.add(new ItemStack(Registration.WHITEOUT_TAPE.get()));
                folders.add(lockedEmptyFolder(tier));
            }
            inputs.add(tapes);
            inputs.add(folders);

            for (int i = 2; i < 9; i++) {
                inputs.add(List.of());
            }

            helper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 1, 1);

            List<ItemStack> outputs = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                outputs.add(unlockedFolder(tier));
            }
            helper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, outputs);
        }
    }

    // -------------------------------------------------------------------------
    // EnderFolderRecipe — 2 folders + Eye of Ender → 2 Ender Folders (same tier)
    // -------------------------------------------------------------------------

    /** Shows: {@code [folder] + [eye of ender] + [folder]} → {@code [ender folder]}, per tier. */
    static final class EnderFolderExtension implements ICraftingCategoryExtension<EnderFolderRecipe> {

        private static final StorageTier[] TIERS = StorageTier.values();

        @Override
        public void setRecipe(
                RecipeHolder<EnderFolderRecipe> recipeHolder,
                IRecipeLayoutBuilder builder,
                ICraftingGridHelper helper,
                IFocusGroup focuses) {
            // Lay out: [folder][eye][folder] in first three slots
            List<List<ItemStack>> inputs = new ArrayList<>();

            List<ItemStack> slot0 = new ArrayList<>();
            List<ItemStack> slot1 = new ArrayList<>();
            List<ItemStack> slot2 = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                slot0.add(unlockedFolder(tier));
                slot1.add(new ItemStack(Items.ENDER_EYE));
                slot2.add(unlockedFolder(tier));
            }
            inputs.add(slot0);
            inputs.add(slot1);
            inputs.add(slot2);

            for (int i = 3; i < 9; i++) {
                inputs.add(List.of());
            }

            helper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 3, 1);

            List<ItemStack> outputs = new ArrayList<>();
            for (StorageTier tier : TIERS) {
                outputs.add(enderFolder(tier));
            }
            helper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, outputs);
        }
    }
}
