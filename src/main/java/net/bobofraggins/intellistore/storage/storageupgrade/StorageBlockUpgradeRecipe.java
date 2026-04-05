package net.bobofraggins.intellistore.storage.storageupgrade;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.storage.StorageTier;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackContents;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting recipe: upgradeable storage block item (any tier) + matching Storage
 * Upgrade → same block item at the next tier, with all stored contents preserved in NBT.
 *
 * <p>The current tier is read from the item's {@code minecraft:block_entity_data} component
 * (the {@code "Tier"} tag written by both block entities on break). The upgrade is consumed and
 * the {@code "Tier"} tag on the result is set to the next tier — all other NBT (stored items,
 * priority, etc.) is carried over unchanged.
 *
 * <p>A single recipe JSON ({@code "type": "intellistore:storage_block_upgrade"}) covers every
 * tier/block combination; the matching logic is entirely in Java.
 */
public class StorageBlockUpgradeRecipe extends CustomRecipe {

    public StorageBlockUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findPair(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] pair = findPair(input);
        if (pair == null) return ItemStack.EMPTY;

        ItemStack blockStack = pair[0];
        StorageUpgradeItem upgradeItem = (StorageUpgradeItem) pair[1].getItem();

        // Tremendous Backpack: upgrade via TremendousBackpackContents data component
        if (blockStack.getItem() instanceof TremendousBackpackItem) {
            TremendousBackpackContents current = blockStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withTier(upgradeItem.getToTier()));
            return result;
        }

        // Block items: copy block_entity_data (preserving stored items / priority) and bump tier
        CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putString("Tier", upgradeItem.getToTier().getId());

        ItemStack result = blockStack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.STORAGE_BLOCK_UPGRADE_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ItemStack[] findPair(CraftingInput input) {
        ItemStack blockItem = ItemStack.EMPTY;
        ItemStack upgrade = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (isStorageBlock(stack.getItem())) {
                if (!blockItem.isEmpty()) return null;
                blockItem = stack;
            } else if (stack.getItem() instanceof StorageUpgradeItem) {
                if (!upgrade.isEmpty()) return null;
                upgrade = stack;
            } else {
                return null;
            }
        }

        if (blockItem.isEmpty() || upgrade.isEmpty()) return null;

        StorageUpgradeItem upgradeItem = (StorageUpgradeItem) upgrade.getItem();
        if (tierFromStack(blockItem) != upgradeItem.getFromTier()) return null;

        return new ItemStack[] {blockItem, upgrade};
    }

    private static boolean isStorageBlock(Item item) {
        return item == Registration.BULK_STORAGE_CONTAINER_ITEM.get() || item instanceof TremendousBackpackItem;
    }

    private static StorageTier tierFromStack(ItemStack stack) {
        // Tremendous Backpack: tier lives in its DataComponent
        if (stack.getItem() instanceof TremendousBackpackItem) {
            TremendousBackpackContents contents = stack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
            return contents.tier();
        }
        // Block items: tier lives in block_entity_data NBT
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            CompoundTag tag = data.getUnsafe();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getString("Tier"));
            }
        }
        return StorageTier.WOOD;
    }
}
