package net.bobofraggins.intellistore.shared.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bobofraggins.intellistore.IntelliStore;
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
 * Client-to-server packet: request the current item list for a Storage Access Terminal.
 *
 * <p>Carries the NI position directly (resolved at menu-open time) so the handler
 * can jump straight to the block entity without re-running BFS on every request.
 * Sent when the SAT screen opens and whenever a transfer completes.
 */
public record RequestSatContentsPacket(BlockPos niPos) implements CustomPacketPayload {

    public static final Type<RequestSatContentsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "request_sat_contents"));

    public static final StreamCodec<FriendlyByteBuf, RequestSatContentsPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RequestSatContentsPacket::niPos, RequestSatContentsPacket::new);

    @Override
    public Type<RequestSatContentsPacket> type() {
        return TYPE;
    }

    public static void handle(RequestSatContentsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            BlockEntity be = player.level().getBlockEntity(packet.niPos());
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) {
                PacketDistributor.sendToPlayer(player, new SatContentsPacket(List.of(), List.of()));
                return;
            }

            IItemHandler handler = ni.getItemHandler();
            if (handler == null) {
                PacketDistributor.sendToPlayer(player, new SatContentsPacket(List.of(), List.of()));
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
    /**
     * A stable hash key for an ItemStack that ignores count.
     *
     * <p>{@link ItemStack#hashItemAndComponents} gives us a good integer hash;
     * we store the representative stack alongside it so we can check for the
     * rare hash-collision case with {@link ItemStack#isSameItemSameComponents}.
     */
    private record StackKey(ItemStack representative, int hash) {
        static StackKey of(ItemStack stack) {
            return new StackKey(stack.copyWithCount(1), ItemStack.hashItemAndComponents(stack));
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof StackKey other
                    && hash == other.hash
                    && ItemStack.isSameItemSameComponents(representative, other.representative);
        }
    }

    public static SatContentsPacket buildContentsPacket(IItemHandler handler) {
        // Aggregate by item type + components — O(n) with HashMap instead of O(n²)
        Map<StackKey, long[]> totals = new HashMap<>();
        int slots = handler.getSlots();
        for (int s = 0; s < slots; s++) {
            ItemStack inSlot = handler.getStackInSlot(s);
            if (inSlot.isEmpty()) continue;
            StackKey key = StackKey.of(inSlot);
            totals.computeIfAbsent(key, k -> new long[1])[0] += inSlot.getCount();
        }

        // Pre-compute display names once to avoid re-materialising per sort comparison
        record Entry(ItemStack stack, long count, String name) {}
        List<Entry> entries = new ArrayList<>(totals.size());
        for (Map.Entry<StackKey, long[]> e : totals.entrySet()) {
            ItemStack rep = e.getKey().representative();
            entries.add(new Entry(rep, e.getValue()[0], rep.getDisplayName().getString()));
        }

        // Sort: highest count first, then display name ascending
        entries.sort(Comparator.comparingLong(Entry::count).reversed().thenComparing(Entry::name));

        List<ItemStack> stacks = new ArrayList<>(entries.size());
        List<Long> counts = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            stacks.add(entry.stack());
            counts.add(entry.count());
        }
        return new SatContentsPacket(List.copyOf(stacks), List.copyOf(counts));
    }
}
