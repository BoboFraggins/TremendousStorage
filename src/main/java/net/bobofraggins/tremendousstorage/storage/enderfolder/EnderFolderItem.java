package net.bobofraggins.tremendousstorage.storage.enderfolder;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An Ender Folder is a Manila Folder whose contents are shared globally across all items that
 * carry the same {@code ENDER_LINK_ID} data component value.
 *
 * <p>The authoritative state lives in {@link EnderFolderStorage} on the server.
 * The {@code FOLDER_CONTENTS} component on the item stack is kept in sync whenever
 * the contents change, so clients always display the correct count.
 *
 * <p>To get the live contents of an Ender Folder item, call
 * {@link #getLiveContents(ItemStack, MinecraftServer)} on the server side.
 * To write updated contents back (and propagate to all linked items in loaded chunks),
 * call {@link #setLiveContents(ItemStack, FolderContents, MinecraftServer)}.
 */
public class EnderFolderItem extends ManillaFolderItem {

    public EnderFolderItem(Properties properties) {
        super(properties);
    }

    // -------------------------------------------------------------------------
    // Link ID helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the link ID stored on this item, or {@code -1} if the item is not linked.
     */
    public static long getLinkId(ItemStack stack) {
        Long id = stack.get(Registration.ENDER_LINK_ID.get());
        return id != null ? id : -1L;
    }

    /**
     * Sets the link ID on the given item stack (in-place).
     */
    public static void setLinkId(ItemStack stack, long linkId) {
        stack.set(Registration.ENDER_LINK_ID.get(), linkId);
    }

    // -------------------------------------------------------------------------
    // Live contents (server-side, go through EnderFolderStorage)
    // -------------------------------------------------------------------------

    /**
     * Returns the live shared {@link FolderContents} for this Ender Folder from the server
     * storage. Falls back to the item's component if the server is unavailable (client-side).
     *
     * <p>If the item's tier is higher than what is recorded in storage (e.g. after a smithing
     * tier upgrade), the storage entry is promoted and the upgrade is propagated to all other
     * linked Ender Folder items in loaded inventories.
     */
    public static FolderContents getLiveContents(ItemStack stack, MinecraftServer server) {
        if (server == null) return ManillaFolderItem.getContents(stack);
        long linkId = getLinkId(stack);
        if (linkId == -1L) return ManillaFolderItem.getContents(stack);
        EnderFolderStorage storage = EnderFolderStorage.get(server);
        FolderContents live = storage.getContents(linkId);
        FolderContents itemContents = ManillaFolderItem.getContents(stack);
        if (itemContents.tier().ordinal() > live.tier().ordinal()) {
            // Item has a higher tier — this happens after a smithing tier upgrade.
            // Promote the storage entry and propagate to all linked items.
            FolderContents promoted = live.withTier(itemContents.tier());
            storage.setContents(linkId, promoted);
            propagateToLoaded(linkId, promoted, server);
            return promoted;
        }
        return live;
    }

    /**
     * Writes updated {@link FolderContents} to the server storage, updates the item stack's
     * component, and propagates the new state to all online players and loaded block entities
     * that hold a linked Ender Folder with the same link ID.
     */
    public static void setLiveContents(ItemStack stack, FolderContents fc, MinecraftServer server) {
        // Always update the item's own component
        stack.set(Registration.FOLDER_CONTENTS.get(), fc);

        if (server == null) return;
        long linkId = getLinkId(stack);
        if (linkId == -1L) return;

        EnderFolderStorage storage = EnderFolderStorage.get(server);
        storage.setContents(linkId, fc);

        // Propagate to all loaded player inventories and filing cabinet BEs
        propagateToLoaded(linkId, fc, server);
    }

    /**
     * Syncs the item's component from the authoritative server storage.
     * Call this when loading an Ender Folder from disk (e.g. player login, chunk load).
     *
     * <p>If the item's tier is higher than what is in storage (e.g. after a smithing tier
     * upgrade), the storage is promoted and the upgrade propagates to all linked items.
     */
    public static void syncFromStorage(ItemStack stack, MinecraftServer server) {
        if (server == null) return;
        long linkId = getLinkId(stack);
        if (linkId == -1L) return;
        EnderFolderStorage storage = EnderFolderStorage.get(server);
        FolderContents live = storage.getContents(linkId);
        FolderContents itemContents = ManillaFolderItem.getContents(stack);
        if (itemContents.tier().ordinal() > live.tier().ordinal()) {
            FolderContents promoted = live.withTier(itemContents.tier());
            storage.setContents(linkId, promoted);
            propagateToLoaded(linkId, promoted, server);
            stack.set(Registration.FOLDER_CONTENTS.get(), promoted);
        } else {
            stack.set(Registration.FOLDER_CONTENTS.get(), live);
        }
    }

    // -------------------------------------------------------------------------
    // Propagation
    // -------------------------------------------------------------------------

    /**
     * Walks all loaded levels → all players → all inventory slots, updating any Ender Folder
     * items whose link ID matches.
     *
     * <p>Filing Cabinet block entities are intentionally not walked here: the
     * {@link net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetItemHandler} reads live
     * contents via {@link #getLiveContents} on every access, so the capability always returns
     * current data even if the item component on the folder stack is stale.
     */
    private static void propagateToLoaded(long linkId, FolderContents fc, MinecraftServer server) {
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            for (Player player : level.players()) {
                updatePlayerInventory(player, linkId, fc);
            }
        }
    }

    private static void updatePlayerInventory(Player player, long linkId, FolderContents fc) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isLinked(s, linkId)) {
                s.set(Registration.FOLDER_CONTENTS.get(), fc);
            }
        }
    }

    private static boolean isLinked(ItemStack stack, long linkId) {
        if (!(stack.getItem() instanceof EnderFolderItem)) return false;
        return getLinkId(stack) == linkId;
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        long linkId = getLinkId(stack);
        if (linkId != -1L) {
            // Show a short hex suffix so players can visually confirm linking
            lines.add(Component.translatable(
                    "item.tremendousstorage.ender_folder.linked",
                    String.format("%016X", linkId).substring(12)));
        }
    }
}
