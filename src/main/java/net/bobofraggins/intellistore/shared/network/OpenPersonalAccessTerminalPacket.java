package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: open the Wireless SAT UI for the given NI position.
 *
 * <p>Sent when the player presses the Wireless SAT keybind. The server validates that
 * the player actually has a linked Wireless SAT for this NI before opening the menu.
 */
public record OpenPersonalAccessTerminalPacket(BlockPos niPos) implements CustomPacketPayload {

    public static final Type<OpenPersonalAccessTerminalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "open_wireless_sat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPersonalAccessTerminalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(), OpenPersonalAccessTerminalPacket::niPos, OpenPersonalAccessTerminalPacket::new);

    @Override
    public Type<OpenPersonalAccessTerminalPacket> type() {
        return TYPE;
    }

    public static void handle(OpenPersonalAccessTerminalPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            PersonalAccessTerminalItem.openSatUi(player, packet.niPos());
        });
    }
}
