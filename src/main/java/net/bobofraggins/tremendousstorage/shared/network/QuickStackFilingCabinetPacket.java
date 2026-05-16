package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.ui.AbstractFilingCabinetMenu;
import net.bobofraggins.tremendousstorage.storage.enderfolder.EnderFolderItem;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "quick_stack_filing_cabinet"));

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
                if (!(folderItem.getItem() instanceof ManillaFolderItem)) continue;

                boolean isEnder = folderItem.getItem() instanceof EnderFolderItem;
                FolderContents contents = isEnder
                        ? EnderFolderItem.getLiveContents(
                                folderItem, ((net.minecraft.server.level.ServerLevel) player.level()).getServer())
                        : ManillaFolderItem.getContents(folderItem);
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
                    ItemStack updatedFolder = folderItem.copy();
                    if (isEnder) {
                        EnderFolderItem.setLiveContents(
                                updatedFolder,
                                contents,
                                ((net.minecraft.server.level.ServerLevel) player.level()).getServer());
                    } else {
                        updatedFolder = ManillaFolderItem.setContents(updatedFolder, contents);
                    }
                    folderSlot.set(updatedFolder);
                }
            }
        });
    }
}
