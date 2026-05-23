package net.bobofraggins.tremendousstorage.shared.crafting;

import net.minecraft.world.item.ItemStack;

/**
 * Strategy interface for the source from which the crafting grid is refilled after a craft.
 *
 * <p>Implementations are responsible for checking side (client vs. server) as appropriate.
 * {@link #extractOne} should return {@link ItemStack#EMPTY} when unavailable on the calling side.
 */
public interface CraftingRefillSource {

    /**
     * Extracts one item matching {@code template} from this source.
     * Returns {@link ItemStack#EMPTY} if not found or unavailable.
     */
    ItemStack extractOne(ItemStack template);

    /**
     * Returns one item to this source (used when swapping a crafting remainder back for
     * the full ingredient — e.g. returning an empty bucket after a lava-bucket craft).
     * Implementations should absorb the item as best they can; if the source is full,
     * the item may be silently discarded (matching previous per-source behaviour).
     */
    void returnOne(ItemStack stack);

    /**
     * Persists any mutations accumulated during this refill pass.
     * Called once after all {@link #extractOne} and {@link #returnOne} calls are done.
     * The default implementation is a no-op; mutable sources (e.g. BackpackContents)
     * override this to write the updated state back to the item stack.
     */
    default void commit(net.minecraft.world.entity.player.Player player) {}
}
