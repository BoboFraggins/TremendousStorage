package net.bobofraggins.tremendousstorage.shared.network;

import java.util.List;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: fill the crafting grid of a Backpack from both player inventory and
 * BackpackContents.
 *
 * <p>Sent by {@link net.bobofraggins.tremendousstorage.external.jei.BackpackJeiTransferHandler}.
 * The server clears the existing crafting grid back to the player, then fills it with the requested
 * items drawn first from player inventory and then from the backpack's stored contents.
 */
public record BackpackFillCraftingGridPacket(int slotType, int slotIndex, String slotId, List<ItemStack> items)
        implements CustomPacketPayload {

    public static final Type<BackpackFillCraftingGridPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "backpack_fill_crafting_grid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackFillCraftingGridPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    BackpackFillCraftingGridPacket::slotType,
                    ByteBufCodecs.INT,
                    BackpackFillCraftingGridPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    BackpackFillCraftingGridPacket::slotId,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    BackpackFillCraftingGridPacket::items,
                    BackpackFillCraftingGridPacket::new);

    @Override
    public Type<BackpackFillCraftingGridPacket> type() {
        return TYPE;
    }

    public static void handle(BackpackFillCraftingGridPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof BackpackMenu menu)) return;
            if (menu.getSlotType() != packet.slotType()) return;
            if (menu.getSlotIndex() != packet.slotIndex()) return;
            if (!menu.getSlotId().equals(packet.slotId())) return;
            if (!menu.hasCraftingUpgrade()) return;

            ItemStack backpackStack =
                    BackpackItem.getBackpackStack(player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;

            BackpackContents contents =
                    backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);

            // Return existing crafting grid contents to the player
            for (int i = 1; i <= 9; i++) {
                ItemStack existing = menu.slots.get(i).getItem();
                if (!existing.isEmpty()) {
                    if (!player.addItem(existing.copy())) player.drop(existing.copy(), false);
                    menu.slots.get(i).set(ItemStack.EMPTY);
                }
            }

            // Fill grid from the 9 desired items (indices 0-8 → slots 1-9)
            List<ItemStack> wanted = packet.items();
            for (int i = 0; i < 9; i++) {
                if (i >= wanted.size()) break;
                ItemStack wantedStack = wanted.get(i);
                if (wantedStack.isEmpty()) continue;

                // Try player inventory first
                ItemStack found = takeFromInventory(player.getInventory(), wantedStack);
                if (found.isEmpty()) {
                    // Try backpack contents
                    int idx = findInContents(contents, wantedStack);
                    if (idx >= 0) {
                        Object[] result = contents.withExtracted(idx, 1);
                        found = (ItemStack) result[0];
                        contents = (BackpackContents) result[1];
                    }
                }

                if (!found.isEmpty()) {
                    menu.slots.get(i + 1).set(found);
                }
            }

            // Save modified contents back to the backpack item
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
            BackpackItem.setBackpackStack(
                    player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());

            // Trigger crafting result update
            menu.slotsChanged(menu.slots.get(1).container);
        });
    }

    private static ItemStack takeFromInventory(Inventory inv, ItemStack wanted) {
        for (int i = 0; i < inv.getNonEquipmentItems().size(); i++) {
            ItemStack stack = inv.getNonEquipmentItems().get(i);
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                ItemStack result = stack.copyWithCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) inv.getNonEquipmentItems().set(i, ItemStack.EMPTY);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int findInContents(BackpackContents contents, ItemStack wanted) {
        StorageKey wantedKey = StorageKey.of(wanted);
        for (int i = 0; i < contents.typeCount(); i++) {
            if (StorageKey.of(contents.getType(i)).equals(wantedKey)) {
                return i;
            }
        }
        return -1;
    }
}
