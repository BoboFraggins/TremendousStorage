package net.bobofraggins.tremendousstorage.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.bobofraggins.tremendousstorage.shared.register.Registration;

/**
 * EMI plugin entry point for TremendousStorage.
 *
 * <p>This class is loaded exclusively by EMI's own class scanner via {@link EmiEntrypoint}.
 * It is never referenced from TremendousStorage's own startup code, so it causes no
 * {@link ClassNotFoundException} when EMI is absent.
 */
@EmiEntrypoint
public class TremendousStorageEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // Register the Access Terminal as a crafting workstation so it appears in EMI's
        // "where can I craft this?" list for vanilla crafting recipes.
        registry.addWorkstation(
                VanillaEmiRecipeCategories.CRAFTING,
                EmiStack.of(Registration.STORAGE_ACCESS_TERMINAL.get().asItem()));

        // Register the recipe transfer handler for the Access Terminal menu.
        registry.addRecipeHandler(Registration.STORAGE_ACCESS_TERMINAL_MENU.get(), new TerminalEmiRecipeHandler());
    }
}
