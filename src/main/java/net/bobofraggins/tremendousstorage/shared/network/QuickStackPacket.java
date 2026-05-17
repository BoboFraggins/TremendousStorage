package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Client-to-server packet for the Quick Stack action.
 *
 * <p>Scans the player's inventory and transfers any item that already exists in the target
 * storage — without introducing new item types. Items that are damageable or have non-default
 * component data (enchantments, etc.) are never transferred this way.
 *
 * <p>When {@code isNetwork} is {@code true}, {@code pos} is a Network Interface position and
 * items are inserted via its {@link IItemHandler}. Otherwise {@code pos} is the local storage
 * block, identified at runtime as a {@link ChestBlockEntity}.
 */
public record QuickStackPacket(BlockPos pos, boolean isNetwork) implements CustomPacketPayload {

    public static final Type<QuickStackPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "quick_stack"));

    public static final StreamCodec<FriendlyByteBuf, QuickStackPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            QuickStackPacket::pos,
            StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean),
            QuickStackPacket::isNetwork,
            QuickStackPacket::new);

    @Override
    public Type<QuickStackPacket> type() {
        return TYPE;
    }

    public static void handle(QuickStackPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (packet.isNetwork()) {
                handleNetwork(packet.pos(), player);
            } else {
                BlockEntity be = player.level().getBlockEntity(packet.pos());
                if (be instanceof ChestBlockEntity bulk) {
                    handleBulk(bulk, player);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Per-storage handlers
    // -------------------------------------------------------------------------

    private static void handleNetwork(BlockPos niPos, ServerPlayer player) {
        BlockEntity be = player.level().getBlockEntity(niPos);
        if (!(be instanceof NetworkInterfaceBlockEntity ni)) return;

        ResourceHandler<ItemResource> handler = ni.getItemHandler();
        if (handler == null) return;

        boolean anyInserted = false;
        int size = player.getInventory().getContainerSize();
        for (int invSlot = 0; invSlot < size; invSlot++) {
            ItemStack stack = player.getInventory().getItem(invSlot);
            if (stack.isEmpty() || isExcluded(stack)) continue;

            // Only transfer if this type already exists in the network
            if (!networkContains(handler, stack)) continue;

            int inserted = handler.insert(0, ItemResource.of(stack), stack.getCount(), null);
            if (inserted > 0) {
                stack.shrink(inserted);
                if (stack.isEmpty()) player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                anyInserted = true;
            }
        }

        if (anyInserted) {
            player.getInventory().setChanged();
            net.bobofraggins.tremendousstorage.shared.storage.KeyCounter refreshed = ni.getCachedInventory();
            if (refreshed != null) {
                PacketDistributor.sendToPlayer(
                        player, RequestSatContentsPacket.buildContentsPacket(refreshed, ni.getFluidStorageKeys()));
            }
        }
    }

    private static void handleBulk(ChestBlockEntity bulk, ServerPlayer player) {
        int size = player.getInventory().getContainerSize();
        for (int invSlot = 0; invSlot < size; invSlot++) {
            ItemStack stack = player.getInventory().getItem(invSlot);
            if (stack.isEmpty() || isExcluded(stack)) continue;

            // Only transfer if this type already exists in bulk storage
            if (!bulkContains(bulk, stack)) continue;

            long remainder = bulk.insert(stack, stack.getCount(), false);
            int inserted = stack.getCount() - (int) remainder;
            if (inserted > 0) {
                stack.shrink(inserted);
                if (stack.isEmpty()) player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                player.getInventory().setChanged();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Items that are damageable or have non-default components are excluded from Quick Stack. */
    private static boolean isExcluded(ItemStack stack) {
        return stack.isDamageableItem() || !stack.isComponentsPatchEmpty();
    }

    private static boolean networkContains(ResourceHandler<ItemResource> handler, ItemStack stack) {
        ItemResource target = ItemResource.of(stack);
        for (int h = 0; h < handler.size(); h++) {
            if (target.equals(handler.getResource(h))) return true;
        }
        return false;
    }

    private static boolean bulkContains(ChestBlockEntity bulk, ItemStack stack) {
        for (int t = 0; t < bulk.typeCount(); t++) {
            if (ItemStack.isSameItemSameComponents(bulk.getType(t), stack)) return true;
        }
        return false;
    }
}
