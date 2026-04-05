package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.ui.AbstractFilingCabinetMenu;
import net.bobofraggins.intellistore.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.intellistore.storage.manillafolder.FolderContents;
import net.bobofraggins.intellistore.storage.manillafolder.ManillaFolderItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet for Quick Stack into a Filing Cabinet or Personal Filing Cabinet.
 *
 * <p>Carries no payload — the server inspects the player's open
 * {@link AbstractFilingCabinetMenu} directly, iterating folder slots 0–7 and inserting
 * matching player inventory items into any folder that is already locked to a type.
 * Unlocked (empty) folders are skipped so Quick Stack never starts a new folder dedication.
 *
 * <p>Works for both the block Filing Cabinet and the item Personal Filing Cabinet because
 * both expose their live folder items through the same menu slot indices.
 */
public record QuickStackFilingCabinetPacket() implements CustomPacketPayload {

    public static final Type<QuickStackFilingCabinetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "quick_stack_filing_cabinet"));

    public static final StreamCodec<FriendlyByteBuf, QuickStackFilingCabinetPacket> STREAM_CODEC =
            StreamCodec.of((buf, ignored) -> {}, buf -> new QuickStackFilingCabinetPacket());

    @Override
    public Type<QuickStackFilingCabinetPacket> type() {
        return TYPE;
    }

    public static void handle(QuickStackFilingCabinetPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof AbstractFilingCabinetMenu fcMenu)) return;

            int invSize = player.getInventory().getContainerSize();

            for (int i = 0; i < AbstractFilingCabinetMenu.FOLDER_SLOTS; i++) {
                Slot folderSlot = fcMenu.getSlot(i);
                ItemStack folderItem = folderSlot.getItem();
                if (folderItem.isEmpty()) continue;

                // Only plain ManillaFolders — Ender Folders have networked storage semantics
                if (!(folderItem.getItem() instanceof ManillaFolderItem)
                        || folderItem.getItem() instanceof EnderFolderItem) continue;

                FolderContents contents = ManillaFolderItem.getContents(folderItem);
                if (contents.isEmpty()) continue; // unlocked — don't start new dedications

                long capacity = ManillaFolderItem.getCapacity(folderItem);
                boolean changed = false;

                for (int invSlot = 0; invSlot < invSize; invSlot++) {
                    ItemStack playerStack = player.getInventory().getItem(invSlot);
                    if (playerStack.isEmpty()
                            || playerStack.isDamageableItem()
                            || !playerStack.isComponentsPatchEmpty()) continue;
                    if (!contents.accepts(playerStack)) continue;

                    FolderContents.InsertResult result = contents.insert(playerStack.getCount(), capacity);
                    int inserted = (int) (playerStack.getCount() - result.remainder());
                    if (inserted <= 0) continue;

                    contents = result.updated();
                    changed = true;
                    playerStack.shrink(inserted);
                    if (playerStack.isEmpty()) player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                    player.getInventory().setChanged();
                }

                if (changed) {
                    folderSlot.set(ManillaFolderItem.setContents(folderItem.copy(), contents));
                }
            }
        });
    }
}
