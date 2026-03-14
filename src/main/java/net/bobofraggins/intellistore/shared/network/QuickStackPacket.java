package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.storage.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet for the Quick Stack action.
 *
 * <p>Scans the player's inventory and transfers any item that already exists in the target
 * storage — without introducing new item types. Items that are damageable or have non-default
 * component data (enchantments, etc.) are never transferred this way.
 *
 * <p>When {@code isNetwork} is {@code true}, {@code pos} is a Network Interface position and
 * items are inserted via its {@link IItemHandler}. Otherwise {@code pos} is the local storage
 * block, identified at runtime as either a {@link BulkStorageContainerBlockEntity} or a
 * {@link JunkDrawerBlockEntity}.
 */
public record QuickStackPacket(BlockPos pos, boolean isNetwork) implements CustomPacketPayload {

    public static final Type<QuickStackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "quick_stack"));

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
                if (be instanceof BulkStorageContainerBlockEntity bulk) {
                    handleBulk(bulk, player);
                } else if (be instanceof JunkDrawerBlockEntity junk) {
                    handleJunk(junk, player);
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

        IItemHandler handler = ni.getItemHandler();
        if (handler == null) return;

        boolean anyInserted = false;
        int size = player.getInventory().getContainerSize();
        for (int invSlot = 0; invSlot < size; invSlot++) {
            ItemStack stack = player.getInventory().getItem(invSlot);
            if (stack.isEmpty() || isExcluded(stack)) continue;

            // Only transfer if this type already exists in the network
            if (!networkContains(handler, stack)) continue;

            ItemStack remainder = handler.insertItem(0, stack.copy(), false);
            int inserted = stack.getCount() - remainder.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                if (stack.isEmpty()) player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                anyInserted = true;
            }
        }

        if (anyInserted) {
            player.getInventory().setChanged();
            IItemHandler refreshed = ni.getItemHandler();
            if (refreshed != null) {
                PacketDistributor.sendToPlayer(player, RequestSatContentsPacket.buildContentsPacket(refreshed));
            }
        }
    }

    private static void handleBulk(BulkStorageContainerBlockEntity bulk, ServerPlayer player) {
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

    /**
     * Junk Drawer handler — included for completeness, but is always a no-op in practice:
     * Quick Stack excludes damageable/component items, which are the only items a Junk Drawer
     * accepts, so no eligible stack will ever match.
     */
    private static void handleJunk(JunkDrawerBlockEntity junk, ServerPlayer player) {
        int size = player.getInventory().getContainerSize();
        for (int invSlot = 0; invSlot < size; invSlot++) {
            ItemStack stack = player.getInventory().getItem(invSlot);
            if (stack.isEmpty() || isExcluded(stack)) continue;

            boolean found = false;
            for (int j = 0; j < junk.size(); j++) {
                if (ItemStack.isSameItemSameComponents(junk.get(j), stack)) {
                    found = true;
                    break;
                }
            }
            if (!found) continue;

            if (!junk.isFull() && junk.addItem(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                player.getInventory().setChanged();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Items that fit in a Junk Drawer (damageable or non-default components) are excluded. */
    private static boolean isExcluded(ItemStack stack) {
        return JunkDrawerBlockEntity.accepts(stack);
    }

    private static boolean networkContains(IItemHandler handler, ItemStack stack) {
        for (int h = 0; h < handler.getSlots(); h++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(h), stack)) return true;
        }
        return false;
    }

    private static boolean bulkContains(BulkStorageContainerBlockEntity bulk, ItemStack stack) {
        for (int t = 0; t < bulk.typeCount(); t++) {
            if (ItemStack.isSameItemSameComponents(bulk.getType(t), stack)) return true;
        }
        return false;
    }
}
