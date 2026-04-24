package net.bobofraggins.tremendousstorage.storage.storageupgrade;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Crafting recipe: storage block item (any tier) + matching Storage Upgrade →
 * same block item at the next tier, with all stored contents preserved.
 *
 * <p>Template slot is left empty. Base slot holds the storage block; addition slot holds
 * the upgrade item. A single recipe JSON covers every tier/block combination; all
 * matching logic is in Java.
 */
public class StorageUpgradeCraftingRecipe implements CraftingRecipe {

    private static final StorageUpgradeCraftingRecipe INSTANCE = new StorageUpgradeCraftingRecipe();

    public static final MapCodec<StorageUpgradeCraftingRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageUpgradeCraftingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

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

    private static boolean isBaseIngredient(ItemStack stack) {
        return isStorageBlock(stack.getItem())
                || isCraftingUpgradeTarget(stack.getItem())
                || isMagnetUpgradeTarget(stack.getItem())
                || isPullerUpgradeTarget(stack.getItem());
    }

    private static boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() instanceof StorageUpgradeItem
                || stack.getItem() instanceof CraftingUpgradeItem
                || stack.getItem() instanceof MagnetUpgradeItem
                || stack.getItem() instanceof HaarpUpgradeItem
                || stack.getItem() instanceof PullerUpgradeItem
                || stack.getItem() instanceof InterdimensionalUpgradeItem;
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
            } else if (addition.isEmpty() && isAdditionIngredient(s)) {
                addition = s;
            } else {
                return false;
            }
        }
        if (base.isEmpty() || addition.isEmpty()) return false;
        if (addition.getItem() instanceof CraftingUpgradeItem) {
            return isCraftingUpgradeTarget(base.getItem()) && !alreadyHasCraftingUpgrade(base);
        }
        if (addition.getItem() instanceof MagnetUpgradeItem) {
            return isMagnetUpgradeTarget(base.getItem()) && !alreadyHasMagnetUpgrade(base);
        }
        if (addition.getItem() instanceof PullerUpgradeItem) {
            return isPullerUpgradeTarget(base.getItem()) && !alreadyHasPullerUpgrade(base);
        }
        if (addition.getItem() instanceof InterdimensionalUpgradeItem) {
            return base.getItem() == Registration.WIRELESS_HUB_ITEM.get()
                    && !alreadyHasInterdimensionalUpgrade(base)
                    && isNetheriteTierItem(base);
        }
        if (addition.getItem() instanceof HaarpUpgradeItem) {
            return base.getItem() == Registration.WIRELESS_HUB.get().asItem() && !alreadyHasHaarpUpgrade(base);
        }
        if (!isStorageBlock(base.getItem())) return false;
        if (!(addition.getItem() instanceof StorageUpgradeItem upgradeItem)) return false;
        return tierFromStack(base) == upgradeItem.getFromTier();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack base = ItemStack.EMPTY;
        ItemStack addition = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (base.isEmpty() && isBaseIngredient(s)) base = s;
            else if (addition.isEmpty() && isAdditionIngredient(s)) addition = s;
        }
        if (addition.getItem() instanceof CraftingUpgradeItem) {
            return applyCraftingUpgrade(base);
        }
        if (addition.getItem() instanceof MagnetUpgradeItem) {
            return applyMagnetUpgrade(base);
        }
        if (addition.getItem() instanceof PullerUpgradeItem) {
            return applyPullerUpgrade(base);
        }
        if (addition.getItem() instanceof InterdimensionalUpgradeItem) {
            return applyInterdimensionalUpgrade(base);
        }
        if (addition.getItem() instanceof HaarpUpgradeItem) {
            return applyHaarpUpgrade(base);
        }
        return upgrade(base, (StorageUpgradeItem) addition.getItem());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Registration.TREMENDOUS_CHEST_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.STORAGE_UPGRADE_CRAFTING_RECIPE.get();
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
        applyBeData(result, tag);
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
        applyBeData(result, tag);
        return result;
    }

    private static boolean isMagnetUpgradeTarget(Item item) {
        return item == Registration.TREMENDOUS_CHEST_ITEM.get()
                || item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof BackpackItem
                || item == Registration.TANK_ITEM.get()
                || item == Registration.ENDER_TANK_ITEM.get()
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
        applyBeData(result, tag);
        return result;
    }

    private static boolean isPullerUpgradeTarget(Item item) {
        return item == Registration.TREMENDOUS_CHEST_ITEM.get()
                || item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get()
                || item instanceof BackpackItem
                || item == Registration.TANK_ITEM.get()
                || item == Registration.ENDER_TANK_ITEM.get()
                || item == Registration.PICNIC_BASKET_ITEM.get()
                || item == Registration.ENDER_PICNIC_BASKET_ITEM.get()
                || item == Registration.FILING_CABINET_ITEM.get();
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
        applyBeData(result, tag);
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
        applyBeData(result, tag);
        return result;
    }

    /**
     * Returns the {@link BlockEntityType} for items that back a block entity, so that
     * {@link BlockItem#setBlockEntityData} can stamp the required {@code "id"} field.
     * Returns {@code null} for items that manage their data via other components (folders,
     * backpack contents) and never need a bare block-entity tag.
     */
    @Nullable
    private static BlockEntityType<?> beTypeForItem(Item item) {
        if (item == Registration.TREMENDOUS_CHEST_ITEM.get()) return Registration.TREMENDOUS_CHEST_BE_TYPE.get();
        if (item == Registration.ENDER_TREMENDOUS_CHEST_ITEM.get())
            return Registration.ENDER_TREMENDOUS_CHEST_BE_TYPE.get();
        if (item == Registration.TANK_ITEM.get()) return Registration.TANK_BE_TYPE.get();
        if (item == Registration.ENDER_TANK_ITEM.get()) return Registration.ENDER_TANK_BE_TYPE.get();
        if (item == Registration.NETWORK_INTERFACE_ITEM.get()) return Registration.NETWORK_INTERFACE_BE_TYPE.get();
        if (item == Registration.WIRELESS_HUB_ITEM.get()) return Registration.WIRELESS_HUB_BE_TYPE.get();
        if (item == Registration.FILING_CABINET_ITEM.get()) return Registration.FILING_CABINET_BE_TYPE.get();
        if (item == Registration.PICNIC_BASKET_ITEM.get()) return Registration.PICNIC_BASKET_BE_TYPE.get();
        if (item == Registration.ENDER_PICNIC_BASKET_ITEM.get()) return Registration.ENDER_PICNIC_BASKET_BE_TYPE.get();
        if (item == Registration.STORAGE_ACCESS_TERMINAL_ITEM.get())
            return Registration.STORAGE_ACCESS_TERMINAL_BE_TYPE.get();
        if (item == Registration.ENDER_TREMENDOUS_BACKPACK_ITEM.get())
            return Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get();
        if (item instanceof BackpackItem) return Registration.TREMENDOUS_BACKPACK_BE_TYPE.get();
        return null;
    }

    /**
     * Sets {@code BLOCK_ENTITY_DATA} on {@code stack}, ensuring the required {@code "id"} field
     * is always present. Uses {@link BlockItem#setBlockEntityData} when the item has a known
     * block-entity type; falls back to raw {@link CustomData} otherwise.
     */
    private static void applyBeData(ItemStack stack, CompoundTag tag) {
        BlockEntityType<?> beType = beTypeForItem(stack.getItem());
        if (beType != null) {
            BlockItem.setBlockEntityData(stack, beType, tag);
        } else {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
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
            // Update BackpackContents (item-form tier/contents).
            // Only update BLOCK_ENTITY_DATA when it already exists (backpack was placed as a block);
            // a fresh unplaced backpack has no block entity data and setting a bare {Tier:...} tag
            // (without the required "id" field) causes ItemStack serialization to crash.
            BackpackContents current =
                    blockStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
            ItemStack result = blockStack.copyWithCount(1);
            result.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withTier(upgradeItem.getToTier()));
            CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (existing != null) {
                CompoundTag beTag = existing.copyTag();
                beTag.putString("Tier", upgradeItem.getToTier().getId());
                applyBeData(result, beTag);
            }
            return result;
        }
        // Chest, tank, NI, and WirelessHub: update "Tier" in BLOCK_ENTITY_DATA (preserving LinkId and other data)
        CustomData existing = blockStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putString("Tier", upgradeItem.getToTier().getId());
        ItemStack result = blockStack.copyWithCount(1);
        applyBeData(result, tag);
        return result;
    }
}
