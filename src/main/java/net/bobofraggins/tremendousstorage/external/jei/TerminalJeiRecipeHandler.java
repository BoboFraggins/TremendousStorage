package net.bobofraggins.tremendousstorage.external.jei;

import java.util.Optional;
import javax.annotation.Nullable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.bobofraggins.tremendousstorage.shared.network.SatFillCraftingGridPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI recipe transfer handler for the Storage Access Terminal.
 *
 * <p>Unlike the default slot-index handler, this fills the crafting grid from the connected
 * network (same as EMI/REI) and passes the specific {@link RecipeHolder} to the menu so that
 * recipe disambiguation works correctly when multiple recipes share the same ingredients.
 */
class TerminalJeiRecipeHandler implements IRecipeTransferHandler<AccessTerminalMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    TerminalJeiRecipeHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends AccessTerminalMenu> getContainerClass() {
        return AccessTerminalMenu.class;
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public Optional<MenuType<AccessTerminalMenu>> getMenuType() {
        return Optional.of(Registration.STORAGE_ACCESS_TERMINAL_MENU.get());
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            AccessTerminalMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (!container.hasNetwork()) {
            return helper.createInternalError();
        }
        if (doTransfer) {
            PacketDistributor.sendToServer(new SatFillCraftingGridPacket(container.getSatPos(), recipe.id()));
        }
        return null;
    }
}
