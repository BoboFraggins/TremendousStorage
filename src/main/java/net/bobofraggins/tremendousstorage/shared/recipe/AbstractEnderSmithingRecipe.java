package net.bobofraggins.tremendousstorage.shared.recipe;

import java.security.SecureRandom;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Base class for the four Ender smithing recipes (Chest, Tank, Folder, Backpack).
 *
 * <p>Handles shared boilerplate: the template/addition ingredient predicates, the
 * {@link #matches} implementation, the thread-local link-ID hand-off between
 * {@link #assemble} and {@link #getRemainingItems}, and the shared {@link SecureRandom}
 * instance.
 *
 * <p>Subclasses provide only the item-type-specific logic: which base items are accepted,
 * how to read an existing link ID, and how to build the ender output item.
 */
public abstract class AbstractEnderSmithingRecipe implements SmithingRecipe {

    protected static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Passes the link ID from {@link #assemble} to {@link #getRemainingItems} so both outputs
     * share the same ID. Safe because Minecraft's crafting pipeline always calls
     * {@code assemble} immediately before {@code getRemainingItems} on the same thread.
     */
    private static final ThreadLocal<long[]> PENDING_LINK = ThreadLocal.withInitial(() -> new long[] {-1L});

    // -------------------------------------------------------------------------
    // Shared ingredient predicates
    // -------------------------------------------------------------------------

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() == Registration.ENDER_STORAGE_UPGRADE.get();
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().isEmpty() && isBaseIngredient(input.base()) && isAdditionIngredient(input.addition());
    }

    // -------------------------------------------------------------------------
    // Shared assembly skeleton
    // -------------------------------------------------------------------------

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        long linkId;
        if (isAlreadyEnder(input.base())) {
            linkId = getExistingLinkId(input.base());
            if (linkId == -1L) linkId = SECURE_RANDOM.nextLong();
        } else {
            linkId = SECURE_RANDOM.nextLong();
        }
        PENDING_LINK.get()[0] = linkId;
        return makeEnderItem(input.base(), linkId);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(SmithingRecipeInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        long linkId = PENDING_LINK.get()[0];
        if (linkId != -1L) {
            PENDING_LINK.get()[0] = -1L;
            remaining.set(1, isAlreadyEnder(input.base()) ? input.base().copy() : makeEnderItem(input.base(), linkId));
        }
        return remaining;
    }

    // -------------------------------------------------------------------------
    // Abstract: per-item-type logic
    // -------------------------------------------------------------------------

    /** Returns {@code true} if {@code stack} is already an ender-linked version of this item. */
    protected abstract boolean isAlreadyEnder(ItemStack stack);

    /**
     * Returns the existing 64-bit link ID stored on {@code stack}, or {@code -1L} if absent.
     * Only called when {@link #isAlreadyEnder} returns {@code true}.
     */
    protected abstract long getExistingLinkId(ItemStack stack);

    /** Builds a new ender output item from {@code base}, stamped with {@code linkId}. */
    protected abstract ItemStack makeEnderItem(ItemStack base, long linkId);
}
