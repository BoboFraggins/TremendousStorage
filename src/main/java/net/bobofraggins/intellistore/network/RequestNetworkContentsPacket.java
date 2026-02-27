package net.bobofraggins.intellistore.network;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.networkinterface.AttachedEntry;
import net.bobofraggins.intellistore.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.networkinterface.NetworkScanResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: request the attached block list for a Network Interface.
 *
 * <p>Sent by the client when the Network Interface screen opens. The server responds
 * with a {@link NetworkContentsPacket} containing the formatted entry strings.
 */
public record RequestNetworkContentsPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestNetworkContentsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "request_network_contents"));

    public static final StreamCodec<FriendlyByteBuf, RequestNetworkContentsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestNetworkContentsPacket::pos,
                    RequestNetworkContentsPacket::new);

    @Override
    public Type<RequestNetworkContentsPacket> type() {
        return TYPE;
    }

    public static void handle(RequestNetworkContentsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) return;

            NetworkScanResult scan = ni.getScan();
            List<String> entries = buildEntries(scan);
            PacketDistributor.sendToPlayer(player, new NetworkContentsPacket(entries));
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Converts a scan result into display strings for the UI. */
    private static List<String> buildEntries(NetworkScanResult scan) {
        if (scan == null) return List.of();
        List<String> result = new ArrayList<>();
        for (AttachedEntry entry : scan.blockList()) {
            String name = translationKeyToName(entry.translationKey());
            result.add(entry.count() > 1 ? name + " (" + entry.count() + ")" : name);
        }
        return result;
    }

    /**
     * Converts a translation key to a readable display name by stripping the prefix.
     * E.g. {@code "block.intellistore.filing_cabinet"} → {@code "Filing Cabinet"}.
     *
     * <p>The server cannot use {@code Component.translatable} for display because the
     * client-side language is not available server-side. Instead we send the raw key and
     * let the client translate it — but for simplicity we send the formatted key.
     * The client will receive the string and display it as-is in the screen.
     *
     * <p>Actually: we send translation keys and the screen translates them on the client.
     * Here we just pass the key unchanged.
     */
    private static String translationKeyToName(String key) {
        // We send the raw translation key; the client resolves it to a display name
        return key;
    }
}
