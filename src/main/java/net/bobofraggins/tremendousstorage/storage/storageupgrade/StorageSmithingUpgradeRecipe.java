package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import com.mojang.serialization.MapCodec;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.CraftingUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.HaarpUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.InterdimensionalUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.MagnetUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.baseupgrade.PullerUpgradeItem;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
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
        return isStorageBlock(stack.getItem())
                || isCraftingUpgradeTarget(stack.getItem())
                || isMagnetUpgradeTarget(stack.getItem())
                || isPullerUpgradeTarget(stack.getItem());
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() instanceof StorageUpgradeItem
                || stack.getItem() instanceof CraftingUpgradeItem
                || stack.getItem() instanceof MagnetUpgradeItem
                || stack.getItem() instanceof HaarpUpgradeItem
                || stack.getItem() instanceof PullerUpgradeItem
                || stack.getItem() instanceof InterdimensionalUpgradeItem;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!input.template().isEmpty()) return false;
        if (input.addition().getItem() instanceof CraftingUpgradeItem) {
            return isCraftingUpgradeTarget(input.base().getItem()) && !alreadyHasCraftingUpgrade(input.base());
        }
        if (input.addition().getItem() instanceof MagnetUpgradeItem) {
            return isMagnetUpgradeTarget(input.base().getItem()) && !alreadyHasMagnetUpgrade(input.base());
        }
        if (input.addition().getItem() instanceof PullerUpgradeItem) {
            return isPullerUpgradeTarget(input.base().getItem()) && !alreadyHasPullerUpgrade(input.base());
        }
        if (input.addition().getItem() instanceof InterdimensionalUpgradeItem) {
            return input.base().getItem() == Registration.WIRELESS_HUB_ITEM.get()
                    && !alreadyHasInterdimensionalUpgrade(input.base())
                    && isNetheriteTierItem(input.base());
        }
        if (input.addition().getItem() instanceof HaarpUpgradeItem) {
            return input.base().getItem() == Registration.WIRELESS_HUB.get().asItem()
                    && !alreadyHasHaarpUpgrade(input.base());
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
        if (input.addition().getItem() instanceof MagnetUpgradeItem) {
            return applyMagnetUpgrade(input.base());
        }
        if (input.addition().getItem() instanceof PullerUpgradeItem) {
            return applyPullerUpgrade(input.base());
        }
        if (input.addition().getItem() instanceof InterdimensionalUpgradeItem) {
            return applyInterdimensionalUpgrade(input.base());
        }
        if (input.addition().getItem() instanceof HaarpUpgradeItem) {
            return applyHaarpUpgrade(input.base());
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
        return item == Registration.TREMENDOUS_CHEST_ITEM.get()
                || item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof BackpackItem
                || item == Registration.MANILA_FOLDER.get()
                || item == Registration.ENDER_FOLDER.get()
                || item == Registration.TANK_ITEM.get()
                || item == Registration.ENDER_TANK_ITEM.get()
                || item == Registration.NETWORK_INTERFACE_ITEM.get()
                || item == Registration.WIRELESS_HUB_ITEM.get();
    }

    private static boolean isCraftingUpgradeTarget(Item item) {
        return item == Registration.TREMENDOUS_CHEST_ITEM.get()
                || item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof BackpackItem // covers both ender and regular backpack
                || item == Registration.STORAGE_ACCESS_TERMINAL_ITEM.get()
                || item == Registration.WIRELESS_SAT.get();
    }

    private static boolean alreadyHasCraftingUpgrade(ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem) {
            return stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY)
                    .hasCraftingUpgrade();
        }
        if (stack.is(Registration.WIRELESS_SAT.get())) {
            return Boolean.TRUE.equals(stack.get(Registration.WIRELESS_SAT_HAS_CRAFTING_UPGRADE.get()));
        }
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.getUnsafe().getBoolean("CraftingUpgrade");
    }

    private static ItemStack applyCraftingUpgrade(ItemStack blockStack) {
        if (blockStack.getItem() instanceof BackpackItem) {
            BackpackContents current =
                    blockStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withCraftingUpgrade());
            return result;
        }
        if (blockStack.is(Registration.WIRELESS_SAT.get())) {
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.WIRELESS_SAT_HAS_CRAFTING_UPGRADE.get(), true);
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

    private static boolean alreadyHasHaarpUpgrade(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.getUnsafe().getBoolean("HaarpUpgrade");
    }

    private static ItemStack applyHaarpUpgrade(ItemStack hubStack) {
        CustomData existing = hubStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putBoolean("HaarpUpgrade", true);
        ItemStack result = hubStack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    private static boolean isMagnetUpgradeTarget(Item item) {
        return item == Registration.TREMENDOUS_CHEST_ITEM.get()
                || item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof BackpackItem
                || item == Registration.FILING_CABINET_ITEM.get();
    }

    private static boolean alreadyHasMagnetUpgrade(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.getUnsafe().getBoolean("MagnetUpgrade");
    }

    private static ItemStack applyMagnetUpgrade(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putBoolean("MagnetUpgrade", true);
        ItemStack result = stack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    private static boolean isPullerUpgradeTarget(Item item) {
        return item == Registration.TREMENDOUS_CHEST_ITEM.get() || item == Registration.FILING_CABINET_ITEM.get();
    }

    private static boolean alreadyHasPullerUpgrade(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.getUnsafe().getBoolean("PullerUpgrade");
    }

    private static ItemStack applyPullerUpgrade(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putBoolean("PullerUpgrade", true);
        ItemStack result = stack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    private static boolean isNetheriteTierItem(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return false;
        return StorageTier.NETHERITE.getId().equals(data.getUnsafe().getString("Tier"));
    }

    private static boolean alreadyHasInterdimensionalUpgrade(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.getUnsafe().getBoolean("InterdimensionalUpgrade");
    }

    private static ItemStack applyInterdimensionalUpgrade(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putBoolean("InterdimensionalUpgrade", true);
        ItemStack result = stack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }

    private static StorageTier tierFromStack(ItemStack stack) {
        if (stack.getItem() instanceof EnderFolderItem || stack.getItem() == Registration.MANILA_FOLDER.get()) {
            return ManillaFolderItem.getContents(stack).tier();
        }
        if (stack.getItem() instanceof BackpackItem) {
            return stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY)
                    .tier();
        }
        // Chest, tank, NI, and WirelessHub store tier in BLOCK_ENTITY_DATA under "Tier"
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
        if (blockStack.getItem() instanceof EnderFolderItem
                || blockStack.getItem() == Registration.MANILA_FOLDER.get()) {
            FolderContents current = ManillaFolderItem.getContents(blockStack);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.FOLDER_CONTENTS.get(), current.withTier(upgradeItem.getToTier()));
            return result;
        }
        if (blockStack.getItem() instanceof BackpackItem) {
            // Update BackpackContents (item-form UI) and BLOCK_ENTITY_DATA (placed-block tier).
            BackpackContents current =
                    blockStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
            CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
            CompoundTag beTag = existing != null ? existing.copyTag() : new CompoundTag();
            beTag.putString("Tier", upgradeItem.getToTier().getId());
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withTier(upgradeItem.getToTier()));
            result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
            return result;
        }
        // Chest, tank, NI, and WirelessHub: update "Tier" in BLOCK_ENTITY_DATA (preserving LinkId and other data)
        CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putString("Tier", upgradeItem.getToTier().getId());
        ItemStack result = blockStack.copyWithCount(1);
        result.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return result;
    }
}
