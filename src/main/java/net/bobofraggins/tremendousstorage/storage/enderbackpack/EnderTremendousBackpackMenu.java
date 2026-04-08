package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tremendousbackpack.TremendousBackpackContents;
import net.bobofraggins.tremendousstorage.storage.tremendousbackpack.TremendousBackpackItem;
import net.bobofraggins.tremendousstorage.storage.tremendousbackpack.TremendousBackpackMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Ender Tremendous Backpack screen.
 *
 * <p>Extends {@link TremendousBackpackMenu} to add post-close sync: when the menu is dismissed,
 * the current {@link TremendousBackpackContents} is written back to {@link EnderBackpackStorage}
 * so the linked partner always sees the latest inventory.
 */
public class EnderTremendousBackpackMenu extends TremendousBackpackMenu {

    private final long linkId;

    /** Server-side constructor. */
    public EnderTremendousBackpackMenu(
            int syncId,
            Inventory playerInv,
            int slotType,
            int slotIndex,
            String slotId,
            ContainerData data,
            boolean hasCraftingUpgrade,
            long linkId) {
        super(
                Registration.ENDER_TREMENDOUS_BACKPACK_MENU.get(),
                syncId,
                playerInv,
                slotType,
                slotIndex,
                slotId,
                data,
                hasCraftingUpgrade,
                TremendousStorageClientConfig.ROWS_SCALE_4_PLUS_DEFAULT);
        this.linkId = linkId;
    }

    /**
     * Client-side constructor. Reads slot location, crafting flag, and linkId from the buffer.
     * The parent buf constructor reads slotType/slotIndex/slotId/craftingUpgrade, then we
     * read the extra linkId from the remaining bytes.
     */
    public EnderTremendousBackpackMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        super(
                Registration.ENDER_TREMENDOUS_BACKPACK_MENU.get(),
                syncId,
                playerInv,
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                new SimpleContainerData(1),
                buf.readBoolean(),
                TremendousStorageClientConfig.getVisibleRowsSafe());
        this.linkId = buf.readLong();
    }

    // -------------------------------------------------------------------------
    // Post-close sync
    // -------------------------------------------------------------------------

    @Override
    protected void onMenuRemoved(Player player) {
        if (linkId == -1L || !(player instanceof ServerPlayer sp)) return;
        ItemStack backpackStack =
                TremendousBackpackItem.getBackpackStack(player, getSlotType(), getSlotIndex(), getSlotId());
        if (backpackStack.isEmpty()) return;
        TremendousBackpackContents contents = backpackStack.getOrDefault(
                Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
        ListTag types = contentsToListTag(contents, sp.level().registryAccess());
        EnderBackpackStorage.get(sp.server).setTypes(linkId, types);
    }

    // -------------------------------------------------------------------------
    // Conversion helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link TremendousBackpackContents} to the "Types" {@link ListTag} format used by
     * {@link EnderBackpackStorage} (same format as {@code TremendousChestBlockEntity.saveTypes()}).
     */
    static ListTag contentsToListTag(TremendousBackpackContents contents, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TremendousBackpackContents.Entry e : contents.entries()) {
            CompoundTag entry = new CompoundTag();
            entry.put("Type", e.type().save(registries));
            entry.putLong("Count", e.count());
            list.add(entry);
        }
        return list;
    }

    /**
     * Reconstructs a {@link TremendousBackpackContents} from a storage {@link ListTag},
     * preserving all settings (tier, priority, sortMode, craftingUpgrade) from {@code base}.
     */
    static TremendousBackpackContents listTagToContents(
            ListTag list, TremendousBackpackContents base, HolderLookup.Provider registries) {
        List<TremendousBackpackContents.Entry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long count = entry.getLong("Count");
            ItemStack.parse(registries, entry.getCompound("Type"))
                    .ifPresent(stack -> entries.add(new TremendousBackpackContents.Entry(stack, count)));
        }
        return new TremendousBackpackContents(
                entries, base.tier(), base.priority(), base.sortMode(), base.hasCraftingUpgrade());
    }
}
