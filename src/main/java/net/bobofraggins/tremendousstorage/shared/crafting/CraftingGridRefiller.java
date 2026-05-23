package net.bobofraggins.tremendousstorage.shared.crafting;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Shared post-craft refill algorithm used by all three crafting-upgrade menus.
 *
 * <p>For each of the 9 grid slots: if the slot was occupied before the craft but is now
 * empty (ingredient consumed), pulls a replacement from {@code source}. Also handles the
 * container-item case (e.g. lava bucket → empty bucket): if the slot contains the crafting
 * remainder of the pre-craft item, swaps it for a fresh full item from the source.
 *
 * <p>Returns {@code true} if any slot was changed; callers are responsible for calling
 * {@code slotsChanged} when the return value is true.
 */
public final class CraftingGridRefiller {

    private CraftingGridRefiller() {}

    public static boolean refillFrom(CraftingRefillSource source, CraftingContainer craftSlots, ItemStack[] snapshot) {
        boolean anyRefilled = false;
        for (int i = 0; i < 9; i++) {
            ItemStack snap = snapshot[i];
            if (snap.isEmpty()) continue;

            ItemStack current = craftSlots.getItem(i);
            if (current.isEmpty()) {
                // Normal case: ingredient was fully consumed — pull a replacement.
                ItemStack extracted = source.extractOne(snap);
                if (!extracted.isEmpty()) {
                    craftSlots.setItem(i, extracted);
                    anyRefilled = true;
                }
            } else {
                // Container-item case: vanilla placed the crafting remainder (e.g. empty
                // bucket) back in the slot. Swap it for a fresh full item from the source.
                var remainderTemplate = snap.getItem().getCraftingRemainder(snap);
                ItemStack remainder = remainderTemplate != null ? remainderTemplate.create() : ItemStack.EMPTY;
                if (!remainder.isEmpty() && ItemStack.isSameItemSameComponents(current, remainder)) {
                    ItemStack extracted = source.extractOne(snap);
                    if (!extracted.isEmpty()) {
                        source.returnOne(current.copyWithCount(1));
                        craftSlots.setItem(i, extracted);
                        anyRefilled = true;
                    }
                }
            }
        }
        return anyRefilled;
    }
}
