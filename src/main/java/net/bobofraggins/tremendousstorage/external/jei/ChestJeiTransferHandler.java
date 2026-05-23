package net.bobofraggins.tremendousstorage.external.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.bobofraggins.tremendousstorage.shared.network.ChestFillCraftingGridPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * JEI recipe transfer handler for Tremendous Chest menus with a crafting upgrade.
 *
 * <p>Sends {@link ChestFillCraftingGridPacket} to the server, which fills the crafting grid
 * from chest storage first and then falls back to the player's inventory.
 */
class ChestJeiTransferHandler implements IRecipeTransferHandler<ChestMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    ChestJeiTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends ChestMenu> getContainerClass() {
        return ChestMenu.class;
    }

    @Override
    public Optional<MenuType<ChestMenu>> getMenuType() {
        return Optional.of(Registration.TREMENDOUS_CHEST_MENU.get());
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            ChestMenu menu,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (!menu.hasCraftingUpgrade()) {
            return helper.createInternalError();
        }

        // Collect one item per input slot from JEI's slot views
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        List<ItemStack> slotItems = new ArrayList<>(9);
        for (IRecipeSlotView slotView : inputSlots) {
            List<ItemStack> candidates = slotView.getItemStacks().collect(Collectors.toList());
            if (candidates.isEmpty()) {
                slotItems.add(ItemStack.EMPTY);
            } else {
                slotItems.add(candidates.get(0).copyWithCount(1));
            }
        }
        while (slotItems.size() < 9) slotItems.add(ItemStack.EMPTY);

        if (doTransfer) {
            ClientPacketDistributor.sendToServer(new ChestFillCraftingGridPacket(menu.getPos(), slotItems));
        }
        return null;
    }
}
