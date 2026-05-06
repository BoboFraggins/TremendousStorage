package net.bobofraggins.tremendousstorage.external.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.bobofraggins.tremendousstorage.shared.network.BackpackFillCraftingGridPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI recipe transfer handler for Tremendous Backpack menus with a crafting upgrade.
 *
 * <p>Unlike the default slot-index handler, this checks both player inventory slots and
 * {@link BackpackContents} when determining ingredient availability. On transfer it sends
 * {@link BackpackFillCraftingGridPacket} to the server to fill the crafting grid from both sources.
 */
class BackpackJeiTransferHandler<T extends BackpackMenu>
        implements IRecipeTransferHandler<T, RecipeHolder<CraftingRecipe>> {

    private final Class<T> containerClass;
    private final MenuType<T> menuType;
    private final IRecipeTransferHandlerHelper helper;

    BackpackJeiTransferHandler(Class<T> containerClass, MenuType<T> menuType, IRecipeTransferHandlerHelper helper) {
        this.containerClass = containerClass;
        this.menuType = menuType;
        this.helper = helper;
    }

    @Override
    public Class<? extends T> getContainerClass() {
        return containerClass;
    }

    @Override
    public Optional<MenuType<T>> getMenuType() {
        return Optional.of(menuType);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            T menu,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (!menu.hasCraftingUpgrade()) {
            return helper.createInternalError();
        }

        ItemStack backpackStack =
                BackpackItem.getBackpackStack(player, menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId());
        BackpackContents contents = backpackStack.isEmpty()
                ? BackpackContents.EMPTY
                : backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);

        // Build map of available items: player inventory + backpack contents
        Map<StorageKey, Long> available = new HashMap<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            StorageKey key = StorageKey.of(stack);
            available.merge(key, (long) stack.getCount(), Long::sum);
        }
        for (BackpackContents.Entry entry : contents.entries()) {
            if (entry.type().isEmpty()) continue;
            StorageKey key = StorageKey.of(entry.type());
            available.merge(key, entry.count(), Long::sum);
        }

        // Walk the 9 input slots to find which item to place in each, tracking consumption
        Map<StorageKey, Long> consumed = new HashMap<>();
        List<ItemStack> slotItems = new ArrayList<>(9);
        List<IRecipeSlotView> missingSlots = new ArrayList<>();

        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        for (IRecipeSlotView slotView : inputSlots) {
            List<ItemStack> candidates = slotView.getItemStacks().collect(Collectors.toList());
            if (candidates.isEmpty()) {
                slotItems.add(ItemStack.EMPTY);
                continue;
            }

            ItemStack chosen = ItemStack.EMPTY;
            for (ItemStack candidate : candidates) {
                if (candidate.isEmpty()) continue;
                StorageKey key = StorageKey.of(candidate);
                long avail = available.getOrDefault(key, 0L);
                long used = consumed.getOrDefault(key, 0L);
                if (avail - used > 0) {
                    chosen = candidate;
                    break;
                }
            }

            if (chosen.isEmpty()) {
                missingSlots.add(slotView);
                slotItems.add(ItemStack.EMPTY);
            } else {
                StorageKey key = StorageKey.of(chosen);
                consumed.merge(key, 1L, Long::sum);
                slotItems.add(chosen.copyWithCount(1));
            }
        }

        // Pad to exactly 9 entries
        while (slotItems.size() < 9) {
            slotItems.add(ItemStack.EMPTY);
        }

        if (!missingSlots.isEmpty()) {
            return helper.createUserErrorWithTooltip(
                    Component.translatable("jei.tooltip.error.recipe.transfer.missing"));
        }

        if (doTransfer) {
            PacketDistributor.sendToServer(new BackpackFillCraftingGridPacket(
                    menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), slotItems));
        }

        return null;
    }
}
