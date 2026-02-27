package net.bobofraggins.intellistore.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storagetransceiver.StorageAccessTerminalBFS;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: request the current item list for a Storage Access Terminal.
 *
 * <p>Sent when the SAT screen opens and whenever a transfer completes. The server
 * aggregates all items in the network (via the connected NI) and responds with a
 * {@link SatContentsPacket}.
 */
public record RequestSatContentsPacket(BlockPos satPos) implements CustomPacketPayload {

    public static final Type<RequestSatContentsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "request_sat_contents"));

    public static final StreamCodec<FriendlyByteBuf, RequestSatContentsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestSatContentsPacket::satPos,
                    RequestSatContentsPacket::new);

    @Override
    public Type<RequestSatContentsPacket> type() {
        return TYPE;
    }

    public static void handle(RequestSatContentsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ServerLevel level = (ServerLevel) player.level();

            BlockPos niPos = StorageAccessTerminalBFS.findNI(level, packet.satPos());
            if (niPos == null) {
                PacketDistributor.sendToPlayer(player,
                        new SatContentsPacket(List.of(), List.of()));
                return;
            }

            BlockEntity be = level.getBlockEntity(niPos);
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) {
                PacketDistributor.sendToPlayer(player,
                        new SatContentsPacket(List.of(), List.of()));
                return;
            }

            IItemHandler handler = ni.getItemHandler();
            if (handler == null) {
                PacketDistributor.sendToPlayer(player,
                        new SatContentsPacket(List.of(), List.of()));
                return;
            }

            PacketDistributor.sendToPlayer(player, buildContentsPacket(handler));
        });
    }

    // -------------------------------------------------------------------------
    // Helper — build aggregated item list
    // -------------------------------------------------------------------------

    /**
     * Aggregates all items in the handler by type+components, sorts by count desc
     * then display name asc, and returns a {@link SatContentsPacket}.
     */
    public static SatContentsPacket buildContentsPacket(IItemHandler handler) {
        // Aggregate by item type + components
        Map<ItemStack, Long> totals = new LinkedHashMap<>();
        for (int s = 0; s < handler.getSlots(); s++) {
            ItemStack inSlot = handler.getStackInSlot(s);
            if (inSlot.isEmpty()) continue;
            ItemStack key = inSlot.copyWithCount(1);

            // Find existing key (can't use Map.get directly — ItemStack.equals checks count)
            boolean found = false;
            for (Map.Entry<ItemStack, Long> entry : totals.entrySet()) {
                if (ItemStack.isSameItemSameComponents(entry.getKey(), key)) {
                    entry.setValue(entry.getValue() + inSlot.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                totals.put(key, (long) inSlot.getCount());
            }
        }

        // Sort: highest count first, then display name ascending
        List<Map.Entry<ItemStack, Long>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort(Comparator
                .<Map.Entry<ItemStack, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(e -> e.getKey().getDisplayName().getString()));

        List<ItemStack> stacks = new ArrayList<>(sorted.size());
        List<Long> counts = new ArrayList<>(sorted.size());
        for (Map.Entry<ItemStack, Long> entry : sorted) {
            stacks.add(entry.getKey());
            counts.add(entry.getValue());
        }
        return new SatContentsPacket(List.copyOf(stacks), List.copyOf(counts));
    }
}
