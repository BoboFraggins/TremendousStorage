package net.bobofraggins.tremendousstorage.storage.enderfolder;

import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles two post-craft corrections for Manila Folder crafting:
 *
 * <ol>
 *   <li><b>Ender Folder sync</b> — {@link FolderStorageRecipe} has no server reference and cannot
 *       call {@link EnderFolderItem#setLiveContents}. This handler writes the updated contents to
 *       shared storage and propagates to all linked Ender Folder items.
 *   <li><b>Bulk item consumption</b> — {@link FolderStorageRecipe#assemble} absorbs the entire
 *       input stack, but vanilla crafting only decrements each input slot by 1. This handler fires
 *       before that decrement and adjusts the item slot so that after vanilla's -1 the slot holds
 *       exactly the unabsorbed remainder (0 if all items fit).
 * </ol>
 */
public final class EnderFolderEvents {

    private EnderFolderEvents() {}

    /**
     * When a player opens any container, sync every Ender Folder slot from live storage so that
     * the component is correct before the player hovers over or interacts with the item.
     * {@code broadcastChanges()} sends the updated slot to the client within one tick.
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        if (server == null) return;
        for (Slot slot : event.getContainer().slots) {
            ItemStack stack = slot.getItem();
            if (stack.getItem() instanceof EnderFolderItem) {
                EnderFolderItem.syncFromStorage(stack, server);
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!(result.getItem() instanceof ManillaFolderItem)) return;

        // Ender Folder: sync to shared storage and propagate to linked folders.
        if (result.getItem() instanceof EnderFolderItem) {
            MinecraftServer server =
                    ((net.minecraft.server.level.ServerLevel) event.getEntity().level()).getServer();
            if (server != null) {
                EnderFolderItem.setLiveContents(result, ManillaFolderItem.getContents(result), server);
            }
        }

        // Bulk consumption: assemble() absorbed N items but vanilla will only remove 1 from the
        // item slot. Find the item slot and set its count to (originalCount - absorbed + 1) so
        // that after vanilla's decrement it contains exactly (originalCount - absorbed).
        Container craftMatrix = event.getInventory();
        ItemStack inputFolder = ItemStack.EMPTY;
        int itemSlot = -1;
        ItemStack inputItem = ItemStack.EMPTY;

        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack s = craftMatrix.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof ManillaFolderItem) {
                inputFolder = s;
            } else {
                itemSlot = i;
                inputItem = s;
            }
        }

        if (inputFolder.isEmpty() || itemSlot < 0) return;

        long inputCount = ManillaFolderItem.getContents(inputFolder).count();
        long resultCount = ManillaFolderItem.getContents(result).count();
        long absorbed = resultCount - inputCount;

        if (absorbed <= 1) return; // vanilla's decrement already handles 1-item absorption

        // Set slot to (remainder + 1); vanilla's decrement leaves remainder in the slot.
        long remainder = inputItem.getCount() - absorbed;
        int setTo = (int) Math.max(1, remainder + 1);
        craftMatrix.setItem(itemSlot, inputItem.copyWithCount(setTo));
    }
}
