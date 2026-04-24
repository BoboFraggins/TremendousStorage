package net.bobofraggins.tremendousstorage.shared.recipe;

import java.security.SecureRandom;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/**
 * Base class for the Ender crafting recipes (Chest, Tank, Folder, Backpack, Picnic Basket).
 *
 * <p>Shapeless 2-ingredient crafting recipe: any base storage item + Ender Storage Upgrade →
 * two linked ender items. The second linked item is returned via {@link #getRemainingItems}
 * in place of the consumed base ingredient, so the player ends up with both copies.
 *
 * <p>Subclasses provide only the item-type-specific logic: which base items are accepted,
 * how to read an existing link ID, and how to build the ender output item.
 */
public abstract class AbstractEnderCraftingRecipe implements CraftingRecipe {

    protected static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Passes the link ID from {@link #assemble} to {@link #getRemainingItems} so both outputs
     * share the same ID. Safe because Minecraft's crafting pipeline always calls
     * {@code assemble} immediately before {@code getRemainingItems} on the same thread.
     */
    private static final ThreadLocal<long[]> PENDING_LINK = ThreadLocal.withInitial(() -> new long[] {-1L});

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack base = ItemStack.EMPTY;
        ItemStack addition = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (base.isEmpty() && isBaseIngredient(s)) {
                base = s;
            } else if (addition.isEmpty() && s.getItem() == Registration.ENDER_STORAGE_UPGRADE.get()) {
                addition = s;
            } else {
                return false;
            }
        }
        return !base.isEmpty() && !addition.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack base = findBase(input);
        long linkId;
        if (isAlreadyEnder(base)) {
            linkId = getExistingLinkId(base);
            if (linkId == -1L) linkId = SECURE_RANDOM.nextLong();
        } else {
            linkId = SECURE_RANDOM.nextLong();
        }
        PENDING_LINK.get()[0] = linkId;
        return makeEnderItem(base, linkId);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        long linkId = PENDING_LINK.get()[0];
        if (linkId != -1L) {
            PENDING_LINK.get()[0] = -1L;
            ItemStack base = findBase(input);
            ItemStack second = isAlreadyEnder(base) ? base.copy() : makeEnderItem(base, linkId);
            remaining.set(findBaseSlot(input), makeSecondEnderItem(second));
        }
        return remaining;
    }

    /**
     * Post-processes the second linked item before it is returned to the player.
     *
     * <p>Instance-specific upgrades (crafting, magnet, puller) are stripped so only the
     * item that was actually upgraded retains them. Tier is left intact because it is a
     * shared property that applies to both linked instances.
     *
     * <p>Subclasses may override to handle additional item-form components (e.g. BackpackContents).
     */
    protected ItemStack makeSecondEnderItem(ItemStack second) {
        CustomData data = second.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return second;
        CompoundTag tag = data.copyTag();
        tag.remove("CraftingUpgrade");
        tag.remove("MagnetUpgrade");
        tag.remove("PullerUpgrade");
        tag.remove("PullerSides");
        second.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return second;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ItemStack findBase(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.isEmpty() && isBaseIngredient(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    private int findBaseSlot(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.isEmpty() && isBaseIngredient(s)) return i;
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Abstract: per-item-type logic
    // -------------------------------------------------------------------------

    /** Returns {@code true} if {@code stack} is a valid base ingredient for this recipe. */
    public abstract boolean isBaseIngredient(ItemStack stack);

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
