package net.bobofraggins.tremendousstorage.shared.network;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
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
 * Client-to-server packet: request the current item list for a Storage Access Terminal.
 *
 * <p>Carries the NI position directly (resolved at menu-open time) so the handler
 * can jump straight to the block entity without re-running BFS on every request.
 * Sent when the SAT screen opens and whenever a transfer completes.
 */
public record RequestSatContentsPacket(BlockPos niPos) implements CustomPacketPayload {

    public static final Type<RequestSatContentsPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "request_sat_contents"));

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
                PacketDistributor.sendToPlayer(player, new SatContentsPacket(List.of(), List.of(), Set.of()));
                return;
            }

            KeyCounter inventory = ni.getCachedInventory();
            if (inventory == null) {
                PacketDistributor.sendToPlayer(player, new SatContentsPacket(List.of(), List.of(), Set.of()));
                return;
            }

            Set<StorageKey> fluidKeys = ni.getFluidStorageKeys();
            PacketDistributor.sendToPlayer(player, buildContentsPacket(inventory, fluidKeys));
        });
    }

    // -------------------------------------------------------------------------
    // Helper — build aggregated item list
    // -------------------------------------------------------------------------

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

    public static SatContentsPacket buildContentsPacket(ResourceHandler<ItemResource> handler) {
        // Aggregate by item type + components — O(n) with HashMap instead of O(n²)
        Map<StackKey, long[]> totals = new HashMap<>();
        for (int s = 0; s < handler.size(); s++) {
            ItemResource res = handler.getResource(s);
            if (res.isEmpty()) continue;
            long amount = handler.getAmountAsLong(s);
            ItemStack inSlot = res.toStack(1);
            StackKey key = StackKey.of(inSlot);
            totals.computeIfAbsent(key, k -> new long[1])[0] += amount;
        }

        // Pre-compute display names and durability once to avoid re-materialising per comparison
        record Entry(ItemStack stack, long count, String name, int durability) {}
        List<Entry> entries = new ArrayList<>(totals.size());
        for (Map.Entry<StackKey, long[]> e : totals.entrySet()) {
            ItemStack rep = e.getKey().representative();
            int durability = rep.getMaxDamage() - rep.getDamageValue();
            entries.add(new Entry(rep, e.getValue()[0], rep.getDisplayName().getString(), durability));
        }

        // Sort: highest count first, then display name ascending, then highest durability first
        entries.sort(Comparator.comparingLong(Entry::count)
                .reversed()
                .thenComparing(Entry::name)
                .thenComparing(Comparator.comparingInt(Entry::durability).reversed()));

        List<ItemStack> stacks = new ArrayList<>(entries.size());
        List<Long> counts = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            stacks.add(entry.stack());
            counts.add(entry.count());
        }
        return new SatContentsPacket(List.copyOf(stacks), List.copyOf(counts), Set.of());
    }

    /** Builds a {@link SatContentsPacket} from a pre-aggregated {@link KeyCounter}. */
    public static SatContentsPacket buildContentsPacket(KeyCounter inventory) {
        return buildContentsPacket(inventory, Set.of());
    }

    /**
     * Builds a {@link SatContentsPacket} from a pre-aggregated {@link KeyCounter}, tagging
     * entries whose {@link StorageKey} appears in {@code fluidKeys} as fluid-backed.
     */
    public static SatContentsPacket buildContentsPacket(KeyCounter inventory, Set<StorageKey> fluidKeys) {
        record Entry(ItemStack stack, long count, String name, int durability) {}
        List<Entry> entries = new ArrayList<>();
        for (Object2LongMap.Entry<StorageKey> e : inventory.allEntries()) {
            long count = e.getLongValue();
            if (count <= 0) continue;
            ItemStack rep = e.getKey().toDisplayStack();
            int durability = rep.getMaxDamage() - rep.getDamageValue();
            entries.add(new Entry(rep, count, rep.getDisplayName().getString(), durability));
        }

        entries.sort(Comparator.comparingLong(Entry::count)
                .reversed()
                .thenComparing(Entry::name)
                .thenComparing(Comparator.comparingInt(Entry::durability).reversed()));

        List<ItemStack> stacks = new ArrayList<>(entries.size());
        List<Long> counts = new ArrayList<>(entries.size());
        Set<Integer> fluidIndices = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            stacks.add(entry.stack());
            counts.add(entry.count());
            if (fluidKeys.contains(StorageKey.of(entry.stack()))) {
                fluidIndices.add(i);
            }
        }
        return new SatContentsPacket(List.copyOf(stacks), List.copyOf(counts), Set.copyOf(fluidIndices));
    }
}
