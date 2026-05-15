package net.bobofraggins.tremendousstorage.shared;

import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackItem;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.FannyPackContents;
import net.bobofraggins.tremendousstorage.glamping.picnicbasket.PicnicBasketItemUtils;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Intercepts item pickup and routes matching items into magnetized storage before they reach the
 * player's inventory.
 *
 * <p>Three storage types are checked in priority order:
 * <ol>
 *   <li>{@link net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackItem} —
 *       always active; absorbs items matching any locked Manila Folder.
 *   <li>Tremendous Backpack with magnet upgrade — absorbs items already stored in the backpack.
 *   <li>Picnic Basket — absorbs food items.
 * </ol>
 *
 * <p>The event is cancelled only when at least one item is absorbed, preventing the remainder
 * from landing in the player's main inventory. Any leftover (partially-absorbed stack) is written
 * back to the {@link ItemEntity} so normal world physics can handle it on the next tick.
 */
public final class ItemPickupInterceptor {

    private ItemPickupInterceptor() {}

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack incoming = itemEntity.getItem();
        if (incoming.isEmpty()) return;

        if (tryFannyPack(player, incoming, itemEntity)
                || tryMagnetBackpack(player, incoming, itemEntity)
                || tryPicnicBasket(player, incoming, itemEntity)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    // -------------------------------------------------------------------------
    // Fanny Pack
    // -------------------------------------------------------------------------

    private static boolean tryFannyPack(Player player, ItemStack incoming, ItemEntity entity) {
        // Scan inventory then curios for the first DankFannyPackItem
        ItemStack fannyStack = ItemStack.EMPTY;
        int slotType = -1, slotIndex = -1;
        String slotId = "";

        var items = player.getInventory().getNonEquipmentItems();
        outer:
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() instanceof DankFannyPackItem) {
                fannyStack = items.get(i);
                slotType = 0;
                slotIndex = i;
                break outer;
            }
        }
        if (fannyStack.isEmpty() && player.getOffhandItem().getItem() instanceof DankFannyPackItem) {
            fannyStack = player.getOffhandItem();
            slotType = 0;
            slotIndex = 40;
        }
        if (fannyStack.isEmpty()) {
            try {
                var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
                if (inv != null) {
                    for (var entry : inv.getCurios().entrySet()) {
                        String sid = entry.getKey();
                        var handler = entry.getValue().getStacks();
                        for (int i = 0; i < handler.getSlots(); i++) {
                            if (handler.getStackInSlot(i).getItem() instanceof DankFannyPackItem) {
                                fannyStack = handler.getStackInSlot(i);
                                slotType = 1;
                                slotIndex = i;
                                slotId = sid;
                                break;
                            }
                        }
                        if (!fannyStack.isEmpty()) break;
                    }
                }
            } catch (NoClassDefFoundError | Exception ignored) {
            }
        }
        if (fannyStack.isEmpty()) return false;

        FannyPackContents contents =
                fannyStack.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);
        NonNullList<ItemStack> folders = contents.toItemList();

        long remaining = incoming.getCount();
        boolean changed = false;
        boolean[] enderSlotModified = new boolean[folders.size()];
        for (int slot = 0; slot < folders.size() && remaining > 0; slot++) {
            ItemStack folderItem = folders.get(slot);
            if (folderItem.isEmpty() || !(folderItem.getItem() instanceof ManillaFolderItem)) continue;
            FolderContents fc = ManillaFolderItem.getContents(folderItem);
            if (fc.isEmpty()) continue;
            if (!fc.accepts(incoming)) continue;
            FolderContents.InsertResult result = fc.insert(remaining, ManillaFolderItem.getCapacity(folderItem));
            long absorbed = remaining - result.remainder();
            if (absorbed > 0) {
                folders.set(slot, ManillaFolderItem.setContents(folderItem.copyWithCount(1), result.updated()));
                remaining = result.remainder();
                changed = true;
                if (folderItem.getItem() instanceof EnderFolderItem) {
                    enderSlotModified[slot] = true;
                }
            }
        }

        if (!changed) return false;

        fannyStack.set(Registration.FANNY_PACK_CONTENTS.get(), contents.withSlots(folders));
        DankFannyPackItem.setFannyPackStack(player, fannyStack, slotType, slotIndex, slotId);

        // Sync ender folders that absorbed items to shared storage.
        net.minecraft.server.MinecraftServer server = player.getServer();
        if (server != null) {
            for (int slot = 0; slot < folders.size(); slot++) {
                if (enderSlotModified[slot]) {
                    ItemStack updated = folders.get(slot);
                    EnderFolderItem.setLiveContents(updated, ManillaFolderItem.getContents(updated), server);
                }
            }
        }

        if (remaining == 0 || contents.voidExcess()) {
            entity.discard();
        } else {
            incoming.setCount((int) remaining);
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Magnetized Backpack
    // -------------------------------------------------------------------------

    private static boolean tryMagnetBackpack(Player player, ItemStack incoming, ItemEntity entity) {
        StorageKey key = StorageKey.of(incoming);

        for (int i = 0; i <= 40; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isMagnetBackpack(stack)) continue;

            BackpackContents contents =
                    stack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);

            // Only absorb if this item type is already stored in the backpack
            boolean alreadyStored = contents.entries().stream()
                    .anyMatch(e -> StorageKey.of(e.type()).equals(key));
            if (!alreadyStored) continue;

            Object[] result = contents.withInserted(incoming, incoming.getCount());
            long remainder = (Long) result[0];
            if (remainder >= incoming.getCount()) continue; // nothing absorbed

            stack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), (BackpackContents) result[1]);
            player.getInventory().setItem(i, stack);

            if (remainder == 0) {
                entity.discard();
            } else {
                incoming.setCount((int) remainder);
            }
            return true;
        }
        return false;
    }

    private static boolean isMagnetBackpack(ItemStack stack) {
        if (stack.isEmpty()
                || !(stack.getItem() instanceof net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem))
            return false;
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.copyTag().getBooleanOr("MagnetUpgrade", false);
    }

    // -------------------------------------------------------------------------
    // Picnic Basket
    // -------------------------------------------------------------------------

    private static boolean tryPicnicBasket(Player player, ItemStack incoming, ItemEntity entity) {
        if (!incoming.has(DataComponents.FOOD)) return false;

        var registries = player.level().registryAccess();
        StorageKey key = StorageKey.of(incoming);

        for (int i = 0; i <= 40; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!PicnicBasketItemUtils.isBasket(stack)) continue;
            if (!PicnicBasketItemUtils.isTypeStored(registries, stack, key)) continue;

            long absorbed =
                    PicnicBasketItemUtils.insertIntoBasketItem(registries, stack, incoming, incoming.getCount());
            if (absorbed <= 0) continue;

            player.getInventory().setItem(i, stack);
            long remaining = incoming.getCount() - absorbed;
            if (remaining == 0) {
                entity.discard();
            } else {
                incoming.setCount((int) remaining);
            }
            return true;
        }
        return false;
    }
}
