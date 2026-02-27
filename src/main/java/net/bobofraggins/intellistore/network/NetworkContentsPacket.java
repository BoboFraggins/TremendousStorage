package net.bobofraggins.intellistore.network;

import java.util.List;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client packet: delivers the Network Interface's block list.
 *
 * <p>Each entry is a translation key (e.g. {@code "block.intellistore.filing_cabinet"})
 * that the screen resolves to a display name. Entries may include a count suffix,
 * or the screen can receive raw keys and format them itself.
 *
 * <p>The client handler stores the entries in {@link #PENDING}; the
 * {@link net.bobofraggins.intellistore.ui.NetworkInterfaceScreen} polls this field
 * on each render tick.
 */
public record NetworkContentsPacket(List<String> entries) implements CustomPacketPayload {

    public static final Type<NetworkContentsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "network_contents"));

    public static final StreamCodec<FriendlyByteBuf, NetworkContentsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    NetworkContentsPacket::entries,
                    NetworkContentsPacket::new);

    @Override
    public Type<NetworkContentsPacket> type() {
        return TYPE;
    }

    /**
     * Latest entries received from the server. The screen reads this on the client thread.
     * Volatile for visibility; the value is replaced atomically (immutable list).
     */
    public static volatile List<String> PENDING = List.of();

    public static void handle(NetworkContentsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> PENDING = List.copyOf(packet.entries()));
    }
}
