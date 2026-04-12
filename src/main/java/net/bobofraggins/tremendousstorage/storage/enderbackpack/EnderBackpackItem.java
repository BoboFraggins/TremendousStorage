package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The Ender Tremendous Backpack item.
 *
 * <p>Extends {@link BackpackItem} and overrides {@link #openUi} to sync the backpack's
 * inventory from {@link EnderBackpackStorage} before opening the menu, so the player always sees
 * the latest shared contents. On close, {@link EnderBackpackMenu#onMenuRemoved} writes
 * any changes back to storage.
 */
public class EnderBackpackItem extends BackpackItem {

    public EnderBackpackItem(Block block) {
        super(block);
    }

    // -------------------------------------------------------------------------
    // Display name
    // -------------------------------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.tremendousstorage.ender_backpack");
    }

    // -------------------------------------------------------------------------
    // openUi — sync from storage before opening
    // -------------------------------------------------------------------------

    @Override
    protected void openUi(ServerPlayer player, ItemStack backpackStack, int slotType, int slotIndex, String slotId) {
        Long linkIdObj = backpackStack.get(Registration.ENDER_LINK_ID.get());
        if (linkIdObj == null) {
            // Fall back to block_entity_data if ENDER_LINK_ID component is absent
            // (e.g. backpack was previously placed and broken before the component was set)
            var bedData = backpackStack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (bedData != null) {
                linkIdObj = bedData.copyTag().getLong("LinkId");
            }
        }
        if (linkIdObj == null || linkIdObj == -1L) {
            // No valid link ID — open as a regular backpack
            super.openUi(player, backpackStack, slotType, slotIndex, slotId);
            return;
        }
        long linkId = linkIdObj;

        BackpackContents contents =
                backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);

        // Sync FROM storage (or seed storage if this is first open)
        EnderBackpackStorage storage = EnderBackpackStorage.get(player.server);
        if (storage.hasLink(linkId)) {
            ListTag saved = storage.getTypes(linkId);
            contents = EnderBackpackMenu.listTagToContents(
                    saved, contents, player.level().registryAccess());
            // Tier-max: if item has higher tier (e.g., after smithing upgrade) push to storage
            net.bobofraggins.tremendousstorage.shared.storage.StorageTier storageTier = storage.getTier(linkId);
            if (contents.tier().ordinal() > storageTier.ordinal()) {
                storage.setTier(linkId, contents.tier());
            } else if (storageTier.ordinal() > contents.tier().ordinal()) {
                contents = contents.withTier(storageTier);
            }
            // Crafting upgrade: OR logic — once any linked copy has it, all get it.
            if (storage.hasCraftingUpgrade(linkId)) {
                contents = contents.withCraftingUpgrade();
            } else if (contents.hasCraftingUpgrade()) {
                storage.setCraftingUpgrade(linkId);
            }
        } else {
            // First open — seed storage with current contents and tier
            ListTag initial =
                    EnderBackpackMenu.contentsToListTag(contents, player.level().registryAccess());
            storage.initLink(linkId, initial, contents.tier(), contents.hasCraftingUpgrade());
        }
        // Write the freshened contents back onto the item before opening the menu
        backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
        BackpackItem.setBackpackStack(player, backpackStack, slotType, slotIndex, slotId);

        boolean craftingUpgrade = contents.hasCraftingUpgrade();
        final long finalLinkId = linkId;
        ContainerData data = new SimpleContainerData(1);

        player.openMenu(new Provider(slotType, slotIndex, slotId, data, craftingUpgrade, finalLinkId), buf -> {
            buf.writeInt(slotType);
            buf.writeInt(slotIndex);
            buf.writeUtf(slotId);
            buf.writeBoolean(craftingUpgrade);
            buf.writeLong(finalLinkId);
        });
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    public static class Provider extends BackpackItem.Provider {

        private final long linkId;

        public Provider(
                int slotType,
                int slotIndex,
                String slotId,
                ContainerData data,
                boolean hasCraftingUpgrade,
                long linkId) {
            super(slotType, slotIndex, slotId, data, hasCraftingUpgrade);
            this.linkId = linkId;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.tremendousstorage.ender_backpack");
        }

        @Override
        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                int syncId,
                net.minecraft.world.entity.player.Inventory inv,
                net.minecraft.world.entity.player.Player player) {
            return new EnderBackpackMenu(
                    syncId, inv, getSlotType(), getSlotIndex(), getSlotId(), getData(), hasCraftingUpgrade(), linkId);
        }
    }
}
