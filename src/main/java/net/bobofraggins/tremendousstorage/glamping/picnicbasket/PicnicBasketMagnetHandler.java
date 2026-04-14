package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles the Picnic Basket's item-form magnet: picks up nearby edible {@link ItemEntity}s when
 * the player is carrying a magnetized basket (in inventory or a Curios charm slot).
 *
 * <p>Runs every 4 ticks server-side. Picks up any food item regardless of whether it is already
 * stored in the basket (contrast with the block-form magnet which only absorbs stored types; see
 * {@link PicnicBasketBlockEntity#magnetAccepts}).
 */
public class PicnicBasketMagnetHandler {

    private PicnicBasketMagnetHandler() {}

    private static final long CAPACITY = 4096L; // StorageTier.WOOD
    private static final int SCAN_INTERVAL = 4;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.level().getGameTime() % SCAN_INTERVAL != 0) return;

        ItemStack basketStack = findMagnetizedBasket(player);
        if (basketStack == null) return;

        ServerLevel level = (ServerLevel) player.level();
        AABB searchArea = player.getBoundingBox().inflate(3);
        List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, searchArea);
        HolderLookup.Provider registries = level.registryAccess();

        for (ItemEntity entity : nearby) {
            if (entity.isRemoved()) continue;
            ItemStack food = entity.getItem();
            if (food.isEmpty() || !food.has(DataComponents.FOOD)) continue;

            int inserted = insertIntoBasketItem(basketStack, food, registries);
            if (inserted <= 0) continue;

            int remaining = food.getCount() - inserted;
            if (remaining <= 0) {
                entity.discard();
            } else {
                entity.setItem(food.copyWithCount(remaining));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Find magnetized basket
    // -------------------------------------------------------------------------

    private static ItemStack findMagnetizedBasket(Player player) {
        // Main inventory (includes offhand at slot 40)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isMagnetizedBasket(stack)) return stack;
        }

        // Curios (soft dependency)
        try {
            var curiosInv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (isMagnetizedBasket(stack)) return stack;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed — skip
        }

        return null;
    }

    private static boolean isMagnetizedBasket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Registration.PICNIC_BASKET_ITEM.get()
                && stack.getItem() != Registration.ENDER_PICNIC_BASKET_ITEM.get()) return false;
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return false;
        return data.copyTag().getBoolean("MagnetUpgrade");
    }

    // -------------------------------------------------------------------------
    // Insert into basket item NBT
    // -------------------------------------------------------------------------

    /**
     * Inserts as many items from {@code food} as possible into the basket item's stored inventory
     * (modifying {@link DataComponents#BLOCK_ENTITY_DATA} in place).
     *
     * @return number of items actually inserted
     */
    private static int insertIntoBasketItem(ItemStack basketStack, ItemStack food, HolderLookup.Provider registries) {
        CustomData existing = basketStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag beTag = existing != null ? existing.copyTag() : new CompoundTag();
        ListTag types = beTag.getList("Types", Tag.TAG_COMPOUND);

        long total = 0;
        for (int i = 0; i < types.size(); i++) {
            total += types.getCompound(i).getLong("Count");
        }

        long space = CAPACITY - total;
        if (space <= 0) return 0;

        int toInsert = (int) Math.min(food.getCount(), space);

        boolean found = false;
        for (int i = 0; i < types.size(); i++) {
            CompoundTag entry = types.getCompound(i);
            ItemStack stored = ItemStack.parseOptional(registries, entry.getCompound("Type"));
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, food)) {
                entry.putLong("Count", entry.getLong("Count") + toInsert);
                found = true;
                break;
            }
        }

        if (!found) {
            CompoundTag entry = new CompoundTag();
            entry.put("Type", food.copyWithCount(1).save(registries));
            entry.putLong("Count", toInsert);
            types.add(entry);
        }

        beTag.put("Types", types);
        basketStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
        return toInsert;
    }
}
