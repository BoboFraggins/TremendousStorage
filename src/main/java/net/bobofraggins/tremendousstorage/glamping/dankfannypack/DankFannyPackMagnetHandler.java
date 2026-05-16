package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import static net.bobofraggins.tremendousstorage.shared.ui.AbstractFilingCabinetMenu.FOLDER_SLOTS;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.network.OpenFannyPackPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Magnet behaviour for the Dank Fanny Pack — runs every server tick per player.
 *
 * <p>Scans a 3-block radius around the player for {@link ItemEntity} instances and absorbs any
 * that match a locked folder, mirroring the Filing Cabinet's magnet-upgrade logic.
 * Only locked folders are targeted; unlocked (empty) folders are never auto-filled.
 */
public final class DankFannyPackMagnetHandler {

    private DankFannyPackMagnetHandler() {}

    private record FannyLocation(ItemStack stack, int slotType, int slotIndex, String slotId) {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        FannyLocation loc = findFannyPack(player);
        if (loc == null) return;

        FannyPackContents contents =
                loc.stack.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);
        NonNullList<ItemStack> folders = contents.toItemList();

        AABB area = player.getBoundingBox().inflate(3);
        boolean changed = false;
        boolean[] enderSlotModified = new boolean[FOLDER_SLOTS];

        for (ItemEntity entity : player.level().getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.isRemoved()) continue;
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;

            long remaining = stack.getCount();
            for (int slot = 0; slot < FOLDER_SLOTS && remaining > 0; slot++) {
                ItemStack folderItem = folders.get(slot);
                if (folderItem.isEmpty() || !(folderItem.getItem() instanceof ManillaFolderItem)) continue;
                FolderContents fc = ManillaFolderItem.getContents(folderItem);
                if (fc.isEmpty()) continue; // unlocked — skip
                if (!fc.accepts(stack)) continue;
                long capacity = ManillaFolderItem.getCapacity(folderItem);
                FolderContents.InsertResult result = fc.insert(remaining, capacity);
                long absorbed = remaining - result.remainder();
                if (absorbed > 0) {
                    ItemStack updated = ManillaFolderItem.setContents(folderItem.copyWithCount(1), result.updated());
                    folders.set(slot, updated);
                    remaining = result.remainder();
                    changed = true;
                    if (folderItem.getItem() instanceof EnderFolderItem) {
                        enderSlotModified[slot] = true;
                    }
                }
            }

            if (remaining < stack.getCount()) {
                if (remaining == 0 || contents.voidExcess()) {
                    entity.discard();
                } else {
                    entity.setItem(stack.copyWithCount((int) remaining));
                }
            }
        }

        if (changed) {
            loc.stack.set(Registration.FANNY_PACK_CONTENTS.get(), contents.withSlots(folders));
            DankFannyPackItem.setFannyPackStack(player, loc.stack, loc.slotType, loc.slotIndex, loc.slotId);

            // Sync only the ender folders that actually absorbed items this tick.
            net.minecraft.server.MinecraftServer server =
                    ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
            if (server != null) {
                for (int slot = 0; slot < FOLDER_SLOTS; slot++) {
                    if (enderSlotModified[slot]) {
                        ItemStack updated = folders.get(slot);
                        EnderFolderItem.setLiveContents(updated, ManillaFolderItem.getContents(updated), server);
                    }
                }
            }
        }
    }

    @Nullable
    private static FannyLocation findFannyPack(Player player) {
        // Main inventory
        var items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() instanceof DankFannyPackItem) {
                return new FannyLocation(items.get(i), OpenFannyPackPacket.SLOT_INVENTORY, i, "");
            }
        }
        // Off-hand
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof DankFannyPackItem) {
            return new FannyLocation(offhand, OpenFannyPackPacket.SLOT_INVENTORY, 40, "");
        }
        // Curios
        try {
            var inv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (inv != null) {
                for (var entry : inv.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack s = handler.getStackInSlot(i);
                        if (s.getItem() instanceof DankFannyPackItem) {
                            return new FannyLocation(s, OpenFannyPackPacket.SLOT_CURIOS, i, slotId);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        return null;
    }
}
