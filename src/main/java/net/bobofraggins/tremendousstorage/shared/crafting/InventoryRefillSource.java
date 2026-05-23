package net.bobofraggins.tremendousstorage.shared.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * {@link CraftingRefillSource} backed by the player's personal inventory.
 * Works on both the client and server side.
 */
public final class InventoryRefillSource implements CraftingRefillSource {

    private final Player player;

    public InventoryRefillSource(Player player) {
        this.player = player;
    }

    @Override
    public ItemStack extractOne(ItemStack template) {
        for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
            ItemStack inv = player.getInventory().getItem(j);
            if (!inv.isEmpty() && ItemStack.isSameItemSameComponents(inv, template)) {
                return inv.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void returnOne(ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
