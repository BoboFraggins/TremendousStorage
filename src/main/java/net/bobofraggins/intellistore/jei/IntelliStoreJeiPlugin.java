package net.bobofraggins.intellistore.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.StorageAccessTerminalMenu;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI plugin for IntelliStore.
 *
 * <p>Registers the Storage Access Terminal as a crafting station so that JEI's recipe
 * transfer ("+" button) fills the SAT's embedded 3×3 crafting grid.
 *
 * <p>This class is only loaded when JEI is present (compileOnly dependency).
 * The {@link JeiPlugin} annotation is JEI's auto-discovery mechanism — no extra
 * registration is needed in the mod's event bus.
 */
@JeiPlugin
public class IntelliStoreJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("intellistore", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    /**
     * Register the SAT block item as a catalyst for the vanilla crafting category so it
     * appears alongside the crafting table and other crafting stations in JEI.
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(
                Registration.STORAGE_ACCESS_TERMINAL_ITEM.get().getDefaultInstance(), RecipeTypes.CRAFTING);
    }

    /**
     * Map SAT menu slots to JEI's vanilla crafting recipe transfer handler.
     *
     * <p>SAT slot layout:
     * <ul>
     *   <li>Slot 0: craft result
     *   <li>Slots 1–9: 3×3 crafting grid (recipe inputs)
     *   <li>Slots 10–36: player main inventory
     *   <li>Slots 37–45: player hotbar
     * </ul>
     */
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(
                StorageAccessTerminalMenu.class,
                Registration.STORAGE_ACCESS_TERMINAL_MENU.get(),
                RecipeTypes.CRAFTING,
                1, // recipe slots start (slot 1 = first craft input)
                9, // recipe slot count  (slots 1-9, 3×3 grid)
                10, // inventory slots start
                36); // inventory slot count (27 main + 9 hotbar)
    }
}
