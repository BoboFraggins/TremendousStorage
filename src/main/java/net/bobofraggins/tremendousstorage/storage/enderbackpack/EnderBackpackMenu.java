package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.config.TremendousStorageClientConfig;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
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
 * <p>Extends {@link BackpackMenu} to add post-close sync: when the menu is dismissed,
 * the current {@link BackpackContents} is written back to {@link EnderBackpackStorage}
 * so the linked partner always sees the latest inventory.
 */
public class EnderBackpackMenu extends BackpackMenu {

    private final long linkId;

    /** Server-side constructor. */
    public EnderBackpackMenu(
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
    public EnderBackpackMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
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
        ItemStack backpackStack = BackpackItem.getBackpackStack(player, getSlotType(), getSlotIndex(), getSlotId());
        if (backpackStack.isEmpty()) return;
        BackpackContents contents =
                backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
        ListTag types = contentsToListTag(contents, sp.level().registryAccess());
        EnderBackpackStorage.get(sp.server).setTypes(linkId, types);
    }

    // -------------------------------------------------------------------------
    // Conversion helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link BackpackContents} to the "Types" {@link ListTag} format used by
     * {@link EnderBackpackStorage} (same format as {@code ChestBlockEntity.saveTypes()}).
     */
    static ListTag contentsToListTag(BackpackContents contents, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (BackpackContents.Entry e : contents.entries()) {
            CompoundTag entry = new CompoundTag();
            entry.put("Type", e.type().save(registries));
            entry.putLong("Count", e.count());
            list.add(entry);
        }
        return list;
    }

    /**
     * Reconstructs a {@link BackpackContents} from a storage {@link ListTag},
     * preserving all settings (tier, priority, sortMode, craftingUpgrade) from {@code base}.
     */
    static BackpackContents listTagToContents(ListTag list, BackpackContents base, HolderLookup.Provider registries) {
        List<BackpackContents.Entry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long count = entry.getLong("Count");
            ItemStack.parse(registries, entry.getCompound("Type"))
                    .ifPresent(stack -> entries.add(new BackpackContents.Entry(stack, count)));
        }
        return new BackpackContents(entries, base.tier(), base.priority(), base.sortMode(), base.hasCraftingUpgrade());
    }
}
