package net.bobofraggins.tremendousstorage.shared.network;

import java.util.List;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: fill the crafting grid of a Tremendous Chest from chest storage and
 * the player's inventory.
 *
 * <p>Sent by {@link net.bobofraggins.tremendousstorage.external.jei.ChestJeiTransferHandler}.
 * The server clears the existing crafting grid back to the chest, then fills it from chest storage
 * first, falling back to the player's inventory for any ingredients the chest could not supply.
 */
public record ChestFillCraftingGridPacket(BlockPos pos, List<ItemStack> items) implements CustomPacketPayload {

    public static final Type<ChestFillCraftingGridPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "chest_fill_crafting_grid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChestFillCraftingGridPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ChestFillCraftingGridPacket::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ChestFillCraftingGridPacket::items,
                    ChestFillCraftingGridPacket::new);

    @Override
    public Type<ChestFillCraftingGridPacket> type() {
        return TYPE;
    }

    public static void handle(ChestFillCraftingGridPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ChestMenu menu)) return;
            if (!menu.getPos().equals(packet.pos())) return;
            if (!menu.hasCraftingUpgrade()) return;
            if (!(player.level().getBlockEntity(packet.pos()) instanceof ChestBlockEntity be)) return;

            // Return existing crafting grid contents to the chest
            for (int i = 1; i <= 9; i++) {
                ItemStack existing = menu.slots.get(i).getItem();
                if (!existing.isEmpty()) {
                    be.insert(existing.copy(), existing.getCount(), false);
                    menu.slots.get(i).set(ItemStack.EMPTY);
                }
            }

            // Fill grid: chest storage first, then player inventory
            List<ItemStack> wanted = packet.items();
            for (int i = 0; i < 9; i++) {
                if (i >= wanted.size()) break;
                ItemStack wantedStack = wanted.get(i);
                if (wantedStack.isEmpty()) continue;

                // Try chest storage first
                ItemStack found = ItemStack.EMPTY;
                for (int t = 0; t < be.typeCount(); t++) {
                    if (ItemStack.isSameItemSameComponents(be.getType(t), wantedStack)) {
                        found = be.extract(t, 1, false);
                        break;
                    }
                }

                // Fall back to player inventory
                if (found.isEmpty()) {
                    found = takeFromInventory(player, wantedStack);
                }

                if (!found.isEmpty()) {
                    menu.slots.get(i + 1).set(found);
                }
            }

            // Trigger crafting result update
            menu.slotsChanged(menu.slots.get(1).container);
        });
    }

    private static ItemStack takeFromInventory(ServerPlayer player, ItemStack wanted) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                ItemStack result = stack.copyWithCount(1);
                stack.shrink(1);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }
}
