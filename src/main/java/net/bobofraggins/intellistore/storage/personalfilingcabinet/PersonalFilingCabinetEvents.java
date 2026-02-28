package net.bobofraggins.intellistore.storage.personalfilingcabinet;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Server-side game-bus event handler for the Personal Filing Cabinet auto-pickup mechanic.
 *
 * <p>Fired in {@link ItemEntityPickupEvent.Pre} — before vanilla pickup logic. The handler
 * scans all PFC items in the player's inventory and routes the picked-up item into the first
 * matching locked folder found, shrinking the item entity's stack directly.
 *
 * <p>Void Excess ON (default): overflow is silently discarded and the event is denied
 * (pickup fully handled).
 * <p>Void Excess OFF: only actually-inserted items are consumed; remainder stays for
 * normal pickup.
 */
public class PersonalFilingCabinetEvents {

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemStack pickedUp = event.getItemEntity().getItem();
        if (pickedUp.isEmpty()) return;

        MinecraftServer server = player.level() instanceof ServerLevel sl ? sl.getServer() : null;

        for (int invSlot = 0; invSlot < player.getInventory().getContainerSize(); invSlot++) {
            ItemStack pfcStack = player.getInventory().getItem(invSlot);
            if (!(pfcStack.getItem() instanceof PersonalFilingCabinetItem)) continue;

            PersonalFilingCabinetContents contents =
                    pfcStack.getOrDefault(Registration.PFC_CONTENTS.get(), PersonalFilingCabinetContents.EMPTY);

            List<ItemStack> folders = new ArrayList<>(contents.folders());
            boolean changed = false;

            for (int fi = 0; fi < folders.size() && !pickedUp.isEmpty(); fi++) {
                ItemStack folderStack = folders.get(fi);
                if (folderStack.isEmpty() || !(folderStack.getItem() instanceof ManillaFolderItem folder)) continue;

                FolderContents fc = folderStack.getItem() instanceof EnderFolderItem
                        ? EnderFolderItem.getLiveContents(folderStack, server)
                        : ManillaFolderItem.getContents(folderStack);

                // Only locked (non-empty) folders participate in auto-pickup
                if (fc.isEmpty() || !fc.accepts(pickedUp)) continue;

                long toInsert = pickedUp.getCount();
                long capacity = folder.getCapacity();
                FolderContents.InsertResult result = fc.insert(toInsert, capacity);
                long inserted = toInsert - result.remainder();
                // If void excess is ON, also consume the overflow (void it)
                long voided = contents.voidExcess() ? result.remainder() : 0L;
                long consumed = inserted + voided;

                if (consumed > 0) {
                    // Shrink the item entity's stack directly (legal per event contract)
                    pickedUp.shrink((int) consumed);
                    ItemStack updatedFolder;
                    if (folderStack.getItem() instanceof EnderFolderItem) {
                        ItemStack copy = folderStack.copy();
                        EnderFolderItem.setLiveContents(copy, result.updated(), server);
                        updatedFolder = copy;
                    } else {
                        updatedFolder = ManillaFolderItem.setContents(folderStack, result.updated());
                    }
                    folders.set(fi, updatedFolder);
                    changed = true;
                }
            }

            if (changed) {
                pfcStack.set(
                        Registration.PFC_CONTENTS.get(),
                        new PersonalFilingCabinetContents(List.copyOf(folders), contents.voidExcess()));
                player.getInventory().setItem(invSlot, pfcStack);
            }

            if (pickedUp.isEmpty()) {
                // The entire stack was consumed by the PFC — deny normal pickup
                event.setCanPickup(TriState.FALSE);
                return;
            }
            // If partial: pickedUp still has items, normal pickup will handle the remainder
        }
    }
}
