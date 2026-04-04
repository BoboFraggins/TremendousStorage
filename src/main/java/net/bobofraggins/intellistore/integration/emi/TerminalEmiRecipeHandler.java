package net.bobofraggins.intellistore.integration.emi;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.bobofraggins.intellistore.shared.network.SatFillCraftingGridPacket;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * EMI recipe transfer handler for the Storage Access Terminal.
 *
 * <p>When the player clicks EMI's transfer button while viewing a vanilla crafting recipe, this
 * handler sends {@link SatFillCraftingGridPacket} to the server, which clears the current grid
 * and refills it with ingredients drawn from the connected network.
 *
 * <p>The "show craftable" filter in EMI only considers the player's personal inventory (the slots
 * visible to EMI), not the network contents — this is the same limitation as vanilla Recipe Book
 * and TSS. A recipe will always be transferable as long as the terminal has a network connection.
 */
public class TerminalEmiRecipeHandler implements EmiRecipeHandler<AccessTerminalMenu> {

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<AccessTerminalMenu> screen) {
        // Use the local player's inventory for EMI's craftable filter.
        return new EmiPlayerInventory(Minecraft.getInstance().player);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        EmiRecipeCategory cat = recipe.getCategory();
        return cat == VanillaEmiRecipeCategories.CRAFTING && recipe.getInputs().size() <= 9;
    }

    @Override
    public boolean alwaysDisplaySupport(EmiRecipe recipe) {
        // Show the transfer button even when the craftable filter is active.
        return true;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<AccessTerminalMenu> context) {
        // Allow transfer whenever the terminal has a network connection.
        return context.getScreenHandler().hasNetwork();
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<AccessTerminalMenu> context) {
        AccessTerminalMenu menu = context.getScreenHandler();
        if (!menu.hasNetwork() || recipe.getId() == null) return false;
        PacketDistributor.sendToServer(new SatFillCraftingGridPacket(menu.getSatPos(), recipe.getId()));
        return true;
    }
}
