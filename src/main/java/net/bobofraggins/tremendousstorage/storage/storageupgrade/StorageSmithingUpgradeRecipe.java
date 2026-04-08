package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.CraftingUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.enderbackpack.EnderTremendousBackpackItem;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.bobofraggins.tremendousstorage.storage.tremendousbackpack.TremendousBackpackContents;
import net.bobofraggins.tremendousstorage.storage.tremendousbackpack.TremendousBackpackItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing-table recipe: storage block item (any tier) + matching Storage Upgrade →
 * same block item at the next tier, with all stored contents preserved.
 *
 * <p>Template slot is left empty. Base slot holds the storage block; addition slot holds
 * the upgrade item. A single recipe JSON covers every tier/block combination; all
 * matching logic is in Java.
 */
public class StorageSmithingUpgradeRecipe implements SmithingRecipe {

    public static final MapCodec<StorageSmithingUpgradeRecipe> CODEC =
            MapCodec.unit(new StorageSmithingUpgradeRecipe());

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageSmithingUpgradeRecipe> STREAM_CODEC =
            StreamCodec.unit(new StorageSmithingUpgradeRecipe());

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return isStorageBlock(stack.getItem());
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() instanceof StorageUpgradeItem || stack.getItem() instanceof CraftingUpgradeItem;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!input.template().isEmpty()) return false;
        if (input.addition().getItem() instanceof CraftingUpgradeItem) {
            return isCraftingUpgradeTarget(input.base().getItem())
                    && !alreadyHasCraftingUpgrade(input.base());
        }
        if (!isStorageBlock(input.base().getItem())) return false;
        if (!(input.addition().getItem() instanceof StorageUpgradeItem upgradeItem)) return false;
        return tierFromStack(input.base()) == upgradeItem.getFromTier();
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        if (input.addition().getItem() instanceof CraftingUpgradeItem) {
            return applyCraftingUpgrade(input.base());
        }
        return upgrade(input.base(), (StorageUpgradeItem) input.addition().getItem());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.TREMENDOUS_CHEST_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.STORAGE_SMITHING_UPGRADE_RECIPE.get();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isStorageBlock(Item item) {
        return item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof EnderTremendousBackpackItem
                || item == Registration.ENDER_FOLDER.get()
                || item == Registration.ENDER_TREMENDOUS_TANK_ITEM.get();
    }

    private static boolean isCraftingUpgradeTarget(Item item) {
        return item == Registration.TREMENDOUS_CHEST_ITEM.get()
                || item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof TremendousBackpackItem; // covers both ender and regular backpack
    }

    private static boolean alreadyHasCraftingUpgrade(ItemStack stack) {
        if (stack.getItem() instanceof TremendousBackpackItem) {
            return stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY)
                    .hasCraftingUpgrade();
        }
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.getUnsafe().getBoolean("CraftingUpgrade");
    }

    private static ItemStack applyCraftingUpgrade(ItemStack blockStack) {
        if (blockStack.getItem() instanceof TremendousBackpackItem) {
            TremendousBackpackContents current = blockStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withCraftingUpgrade());
            return result;
        }
        // Chest item: set CraftingUpgrade in block_entity_data
        CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putBoolean("CraftingUpgrade", true);
        ItemStack result = blockStack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    private static StorageTier tierFromStack(ItemStack stack) {
        if (stack.getItem() instanceof EnderFolderItem) {
            return ManillaFolderItem.getContents(stack).tier();
        }
        if (stack.getItem() instanceof EnderTremendousBackpackItem) {
            return stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY)
                    .tier();
        }
        // Ender chest and ender tank store tier in BLOCK_ENTITY_DATA under "Tier"
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            CompoundTag tag = data.getUnsafe();
            if (tag.contains("Tier")) {
                return StorageTier.fromId(tag.getString("Tier"));
            }
        }
        return StorageTier.WOOD;
    }

    private static ItemStack upgrade(ItemStack blockStack, StorageUpgradeItem upgradeItem) {
        if (blockStack.getItem() instanceof EnderFolderItem) {
            FolderContents current = ManillaFolderItem.getContents(blockStack);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.FOLDER_CONTENTS.get(), current.withTier(upgradeItem.getToTier()));
            return result;
        }
        if (blockStack.getItem() instanceof EnderTremendousBackpackItem) {
            TremendousBackpackContents current = blockStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withTier(upgradeItem.getToTier()));
            return result;
        }
        // Ender chest and ender tank: update "Tier" in BLOCK_ENTITY_DATA (preserving LinkId and all other data)
        CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putString("Tier", upgradeItem.getToTier().getId());
        ItemStack result = blockStack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }
}
