package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Static helpers for accessing a picnic basket ItemStack from a player's inventory or Curios slot,
 * and for inserting into / extracting from its {@link DataComponents#BLOCK_ENTITY_DATA} NBT.
 */
public final class PicnicBasketItemUtils {

    /** slotType value for a regular player inventory slot (indices 0-40). */
    public static final int SLOT_INVENTORY = 0;

    /** slotType value for a Curios slot. */
    public static final int SLOT_CURIOS = 1;

    private PicnicBasketItemUtils() {}

    // -------------------------------------------------------------------------
    // Slot access
    // -------------------------------------------------------------------------

    public static ItemStack getBasketStack(Player player, int slotType, int slotIndex, String slotId) {
        if (slotType == SLOT_INVENTORY) {
            ItemStack stack = player.getInventory().getItem(slotIndex);
            return isBasket(stack) ? stack : ItemStack.EMPTY;
        } else {
            return getFromCurios(player, slotIndex, slotId);
        }
    }

    public static void setBasketStack(Player player, ItemStack stack, int slotType, int slotIndex, String slotId) {
        if (slotType == SLOT_INVENTORY) {
            player.getInventory().setItem(slotIndex, stack);
        } else {
            setInCurios(player, stack, slotIndex, slotId);
        }
    }

    public static boolean isBasket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Registration.PICNIC_BASKET_ITEM.get()
                || stack.getItem() == Registration.ENDER_PICNIC_BASKET_ITEM.get();
    }

    private static ItemStack getFromCurios(Player player, int slotIndex, String slotId) {
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv == null) return ItemStack.EMPTY;
            var entry = inv.getCurios().get(slotId);
            if (entry == null) return ItemStack.EMPTY;
            ItemStack stack = entry.getStacks().getStackInSlot(slotIndex);
            return isBasket(stack) ? stack : ItemStack.EMPTY;
        } catch (LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static void setInCurios(Player player, ItemStack stack, int slotIndex, String slotId) {
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv == null) return;
            var entry = inv.getCurios().get(slotId);
            if (entry == null) return;
            entry.getStacks().setStackInSlot(slotIndex, stack);
        } catch (LinkageError ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // Insert / extract
    // -------------------------------------------------------------------------

    /** Returns true if the basket already has an entry whose StorageKey matches {@code key}. */
    public static boolean isTypeStored(HolderLookup.Provider registries, ItemStack basketStack, StorageKey key) {
        CustomData data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return false;
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag types = data.copyTag().getListOrEmpty("Types");
        for (int i = 0; i < types.size(); i++) {
            ItemStack stored = ItemStack.OPTIONAL_CODEC
                    .parse(ops, types.getCompoundOrEmpty(i).getCompoundOrEmpty("Type"))
                    .result()
                    .orElse(ItemStack.EMPTY);
            if (!stored.isEmpty() && StorageKey.of(stored).equals(key)) return true;
        }
        return false;
    }

    /**
     * Inserts up to {@code amount} of {@code toInsert} into the basket's BLOCK_ENTITY_DATA.
     * Modifies {@code basketStack} in place. Returns the number of items actually inserted.
     */
    public static long insertIntoBasketItem(
            HolderLookup.Provider registries, ItemStack basketStack, ItemStack toInsert, long amount) {
        if (!toInsert.has(DataComponents.FOOD)) return 0;

        CustomData data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag beTag = data != null ? data.copyTag() : new CompoundTag();

        StorageTier tier = StorageTier.fromId(beTag.getStringOr("Tier", ""));
        long capacity = tier.getCapacity();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);

        ListTag types = beTag.getListOrEmpty("Types");
        long totalCount = 0;
        for (int i = 0; i < types.size(); i++) {
            totalCount += types.getCompoundOrEmpty(i).getLongOr("Count", 0L);
        }
        long space = capacity - totalCount;
        if (space <= 0) return 0;

        long toInsertAmt = Math.min(amount, space);
        StorageKey key = StorageKey.of(toInsert);
        boolean found = false;
        for (int i = 0; i < types.size(); i++) {
            CompoundTag entry = types.getCompoundOrEmpty(i);
            ItemStack stored = ItemStack.OPTIONAL_CODEC
                    .parse(ops, entry.getCompoundOrEmpty("Type"))
                    .result()
                    .orElse(ItemStack.EMPTY);
            if (!stored.isEmpty() && StorageKey.of(stored).equals(key)) {
                entry.putLong("Count", entry.getLongOr("Count", 0L) + toInsertAmt);
                found = true;
                break;
            }
        }
        if (!found) {
            CompoundTag entry = new CompoundTag();
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, toInsert).result().ifPresent(t -> entry.put("Type", t));
            entry.putLong("Count", toInsertAmt);
            types.add(entry);
        }

        beTag.put("Types", types);
        basketStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
        return toInsertAmt;
    }

    /**
     * Extracts up to {@code amount} items at {@code typeIndex} from the basket's BLOCK_ENTITY_DATA.
     * Modifies {@code basketStack} in place. Returns the extracted stack, or EMPTY.
     */
    public static ItemStack extractFromBasketItem(
            HolderLookup.Provider registries, ItemStack basketStack, int typeIndex, long amount) {
        CustomData data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return ItemStack.EMPTY;

        CompoundTag beTag = data.copyTag();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag types = beTag.getListOrEmpty("Types");

        if (typeIndex < 0 || typeIndex >= types.size()) return ItemStack.EMPTY;

        CompoundTag entry = types.getCompoundOrEmpty(typeIndex);
        long count = entry.getLongOr("Count", 0L);
        if (count == 0) return ItemStack.EMPTY;

        ItemStack stored = ItemStack.OPTIONAL_CODEC
                .parse(ops, entry.getCompoundOrEmpty("Type"))
                .result()
                .orElse(ItemStack.EMPTY);
        if (stored.isEmpty()) return ItemStack.EMPTY;

        long toExtract = Math.min(amount, Math.min(stored.getMaxStackSize(), count));
        if (toExtract <= 0) return ItemStack.EMPTY;

        long remaining = count - toExtract;
        if (remaining <= 0) {
            types.remove(typeIndex);
        } else {
            entry.putLong("Count", remaining);
        }

        beTag.put("Types", types);
        basketStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
        return stored.copyWithCount((int) toExtract);
    }

    // -------------------------------------------------------------------------
    // AutoFeed flag
    // -------------------------------------------------------------------------

    /** Reads the AutoFeed flag from the basket's BLOCK_ENTITY_DATA (defaults to {@code true}). */
    public static boolean readAutoFeed(ItemStack basketStack) {
        CustomData data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return true;
        CompoundTag tag = data.copyTag();
        return tag.getBooleanOr("AutoFeed", true);
    }

    /** Writes the AutoFeed flag to the basket's BLOCK_ENTITY_DATA. */
    public static void writeAutoFeed(ItemStack basketStack, boolean autoFeed) {
        CustomData data = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        tag.putBoolean("AutoFeed", autoFeed);
        basketStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }
}
