package net.bobofraggins.tremendousstorage.storage.enderfolder;

import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Syncs Ender Folder contents to {@link EnderFolderStorage} when a crafting recipe produces an
 * Ender Folder as its output.
 *
 * <p>{@link net.bobofraggins.tremendousstorage.storage.manillafolder.FolderStorageRecipe} only
 * modifies the item's {@code FOLDER_CONTENTS} component; it has no server reference and cannot
 * call {@link EnderFolderItem#setLiveContents}. This handler bridges that gap by writing the
 * updated contents to shared storage and propagating to all linked Ender Folder items.
 */
public final class EnderFolderEvents {

    private EnderFolderEvents() {}

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!(result.getItem() instanceof EnderFolderItem)) return;
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        EnderFolderItem.setLiveContents(result, ManillaFolderItem.getContents(result), server);
    }
}
