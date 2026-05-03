package net.bobofraggins.tremendousstorage.external.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.bobofraggins.tremendousstorage.shared.network.SetImportExportFilterPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.util.SearchSync;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderExtractRecipe;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderMergeRecipe;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderStorageRecipe;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ExportInterfaceScreen;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.ImportInterfaceScreen;
import net.bobofraggins.tremendousstorage.storage.whiteout.FolderTapeRecipe;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI plugin for TremendousStorage.
 *
 * <p>Registers the Storage Access Terminal as a crafting station, and adds ghost ingredient
 * drag support for Import Interface and Export Interface filter screens.
 */
@JeiPlugin
public class TremendousStorageJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        IGuiHelper guiHelper = reg.getJeiHelpers().getGuiHelper();
        reg.addRecipeCategories(
                new PositiveVibesCauldronCategory(guiHelper),
                new EnderFolderCraftingCategory(guiHelper),
                new EnderStorageCraftingCategory(guiHelper),
                new TankExtractionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addRecipes(
                PositiveVibesCauldronCategory.RECIPE_TYPE,
                List.of(
                        PositiveVibesCauldronJeiRecipe.obtainedByRecycling(),
                        PositiveVibesCauldronJeiRecipe.healBrain()));
        reg.addRecipes(EnderFolderCraftingCategory.RECIPE_TYPE, List.of(EnderFolderCraftingCategory.Recipe.INSTANCE));
        reg.addRecipes(
                EnderStorageCraftingCategory.RECIPE_TYPE,
                List.of(
                        EnderStorageCraftingCategory.Recipe.CHEST,
                        EnderStorageCraftingCategory.Recipe.BACKPACK,
                        EnderStorageCraftingCategory.Recipe.TANK,
                        EnderStorageCraftingCategory.Recipe.PICNIC_BASKET,
                        EnderStorageCraftingCategory.Recipe.BARREL));

        reg.addRecipes(
                TankExtractionCategory.RECIPE_TYPE,
                List.of(TankExtractionJeiRecipe.bottleOEnchanting(), TankExtractionJeiRecipe.xpJuiceBucket()));

        IVanillaRecipeFactory brewFactory = reg.getJeiHelpers().getVanillaRecipeFactory();
        reg.addRecipes(
                RecipeTypes.BREWING,
                List.of(
                        brewFactory.createBrewingRecipe(
                                List.of(new ItemStack(Items.REDSTONE)),
                                new ItemStack(Registration.VEX_REPELLENT_POTION.get()),
                                new ItemStack(Registration.VEX_REPELLENT_POTION_EXTENDED.get())),
                        brewFactory.createBrewingRecipe(
                                List.of(new ItemStack(Items.REDSTONE)),
                                new ItemStack(Registration.VEX_REPELLENT_POTION_EXTENDED.get()),
                                new ItemStack(Registration.VEX_REPELLENT_POTION_LONG.get()))));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(
                Registration.STORAGE_ACCESS_TERMINAL_ITEM.get().getDefaultInstance(), RecipeTypes.CRAFTING);
        reg.addRecipeCatalyst(
                Registration.POSITIVE_VIBES_CAULDRON_ITEM.get().getDefaultInstance(),
                PositiveVibesCauldronCategory.RECIPE_TYPE);
        reg.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), EnderFolderCraftingCategory.RECIPE_TYPE);
        reg.addRecipeCatalyst(
                Registration.ENDER_STORAGE_UPGRADE.get().getDefaultInstance(), EnderFolderCraftingCategory.RECIPE_TYPE);
        reg.addRecipeCatalyst(XpJuiceTankItem.create(), TankExtractionCategory.RECIPE_TYPE);
        reg.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), EnderStorageCraftingCategory.RECIPE_TYPE);
        reg.addRecipeCatalyst(
                Registration.ENDER_STORAGE_UPGRADE.get().getDefaultInstance(),
                EnderStorageCraftingCategory.RECIPE_TYPE);
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration reg) {
        reg.getCraftingCategory()
                .addExtension(FolderStorageRecipe.class, new FolderRecipeExtensions.StorageExtension());
        reg.getCraftingCategory()
                .addExtension(FolderExtractRecipe.class, new FolderRecipeExtensions.ExtractExtension());
        reg.getCraftingCategory().addExtension(FolderMergeRecipe.class, new FolderRecipeExtensions.MergeExtension());
        reg.getCraftingCategory().addExtension(FolderTapeRecipe.class, new FolderRecipeExtensions.TapeExtension());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new TerminalJeiRecipeHandler(reg.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    /**
     * Registers ghost ingredient handlers so players can drag items from JEI directly
     * into the 9 filter slots of the Import Interface and Export Interface screens.
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        SearchSync.setProvider(runtime.getIngredientFilter()::getFilterText);
        SearchSync.setJeiSetter(runtime.getIngredientFilter()::setFilterText);
    }

    @Override
    public void onRuntimeUnavailable() {
        SearchSync.setProvider(null);
        SearchSync.setJeiSetter(null);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addGhostIngredientHandler(ImportInterfaceScreen.class, new InterfaceGhostHandler<>());
        reg.addGhostIngredientHandler(ExportInterfaceScreen.class, new InterfaceGhostHandler<>());
    }

    // -------------------------------------------------------------------------
    // Ghost ingredient handler — shared by both screens via duck typing
    // -------------------------------------------------------------------------

    /**
     * Handles JEI ingredient drags into Import/Export Interface filter slot grids.
     * Works for both {@link ImportInterfaceScreen} and {@link ExportInterfaceScreen}.
     */
    private static final class InterfaceGhostHandler<
                    S extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>>
            implements IGhostIngredientHandler<S> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(S screen, ITypedIngredient<I> ingredient, boolean doStart) {
            // Only handle ItemStack ingredients
            Optional<ItemStack> itemOpt = ingredient.getItemStack();
            if (itemOpt.isEmpty()) return List.of();

            List<Target<I>> targets = new ArrayList<>(9);

            for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
                final int si = slotIndex;
                int sx, sy;

                if (screen instanceof ImportInterfaceScreen is) {
                    sx = is.getGhostSlotX(si);
                    sy = is.getGhostSlotY(si);
                } else if (screen instanceof ExportInterfaceScreen es) {
                    sx = es.getGhostSlotX(si);
                    sy = es.getGhostSlotY(si);
                } else {
                    continue;
                }

                int size = ImportInterfaceScreen.getGhostSlotInnerSize();
                Rect2i area = new Rect2i(sx, sy, size, size);

                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return area;
                    }

                    @Override
                    public void accept(I rawIngredient) {
                        if (!(rawIngredient instanceof ItemStack itemStack)) return;
                        ItemStack ghost = itemStack.copyWithCount(1);

                        // Update menu client-side and notify server
                        if (screen instanceof ImportInterfaceScreen is) {
                            is.getMenu().setFilterSlot(si, ghost);
                            PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                                    is.getMenu().getPos(), is.getMenu().getFaceIndex(), si, ghost));
                        } else if (screen instanceof ExportInterfaceScreen es) {
                            es.getMenu().setFilterSlot(si, ghost);
                            PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                                    es.getMenu().getPos(), es.getMenu().getFaceIndex(), si, ghost));
                        }
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
            // Nothing to clean up when the drag operation completes
        }
    }
}
