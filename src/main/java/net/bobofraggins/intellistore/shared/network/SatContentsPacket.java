package net.bobofraggins.intellistore.shared.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client packet: delivers the aggregated item list for a Storage Access Terminal.
 *
 * <p>Parallel lists: {@code stacks} holds one representative {@link ItemStack} per item type
 * (count=1), and {@code counts} holds the total quantity of that type across the whole network.
 *
 * <p>{@code fluidIndices} is the set of indices (into {@code stacks}/{@code counts}) that are
 * backed by a fluid tank and require an empty bucket in the cursor to extract.
 *
 * <p>The client handler stores the lists in {@link #PENDING_STACKS}, {@link #PENDING_COUNTS},
 * and {@link #PENDING_FLUID_INDICES};
 * {@link net.bobofraggins.intellistore.shared.ui.AccessTerminalScreen} polls these each tick.
 */
public record SatContentsPacket(List<ItemStack> stacks, List<Long> counts, Set<Integer> fluidIndices)
        implements CustomPacketPayload {

    public static final Type<SatContentsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "sat_contents"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Long>> LONG_LIST_CODEC = StreamCodec.of(
            (buf, list) -> {
                buf.writeVarInt(list.size());
                for (long v : list) buf.writeVarLong(v);
            },
            buf -> {
                int size = buf.readVarInt();
                List<Long> out = new ArrayList<>(size);
                for (int i = 0; i < size; i++) out.add(buf.readVarLong());
                return out;
            });

    private static final StreamCodec<RegistryFriendlyByteBuf, Set<Integer>> INT_SET_CODEC = StreamCodec.of(
            (buf, set) -> {
                buf.writeVarInt(set.size());
                for (int i : set) buf.writeVarInt(i);
            },
            buf -> {
                int size = buf.readVarInt();
                Set<Integer> out = new HashSet<>(size);
                for (int i = 0; i < size; i++) out.add(buf.readVarInt());
                return out;
            });

    public static final StreamCodec<RegistryFriendlyByteBuf, SatContentsPacket> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            SatContentsPacket::stacks,
            LONG_LIST_CODEC,
            SatContentsPacket::counts,
            INT_SET_CODEC,
            SatContentsPacket::fluidIndices,
            SatContentsPacket::new);

    @Override
    public Type<SatContentsPacket> type() {
        return TYPE;
    }

    /** Latest item stacks received from the server. Client-thread only. Null until first packet arrives. */
    public static volatile List<ItemStack> PENDING_STACKS = null;

    /** Parallel to {@link #PENDING_STACKS}: total count of each item type. */
    public static volatile List<Long> PENDING_COUNTS = List.of();

    /** Indices (into the sorted stacks list) that are fluid-backed and require a bucket to extract. */
    public static volatile Set<Integer> PENDING_FLUID_INDICES = Set.of();

    public static void handle(SatContentsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PENDING_STACKS = List.copyOf(packet.stacks());
            PENDING_COUNTS = List.copyOf(packet.counts());
            PENDING_FLUID_INDICES = Set.copyOf(packet.fluidIndices());
        });
    }
}
