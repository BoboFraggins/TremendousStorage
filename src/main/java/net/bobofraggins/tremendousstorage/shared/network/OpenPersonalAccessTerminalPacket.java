package net.bobofraggins.tremendousstorage.shared.network;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalItem;
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
public record OpenPersonalAccessTerminalPacket(BlockPos niPos, @Nullable BlockPos hubPos)
        implements CustomPacketPayload {

    public static final Type<OpenPersonalAccessTerminalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "open_wireless_sat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPersonalAccessTerminalPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenPersonalAccessTerminalPacket decode(RegistryFriendlyByteBuf buf) {
                    BlockPos niPos = BlockPos.STREAM_CODEC.decode(buf);
                    BlockPos hubPos = buf.readBoolean() ? BlockPos.STREAM_CODEC.decode(buf) : null;
                    return new OpenPersonalAccessTerminalPacket(niPos, hubPos);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, OpenPersonalAccessTerminalPacket value) {
                    BlockPos.STREAM_CODEC.encode(buf, value.niPos());
                    if (value.hubPos() != null) {
                        buf.writeBoolean(true);
                        BlockPos.STREAM_CODEC.encode(buf, value.hubPos());
                    } else {
                        buf.writeBoolean(false);
                    }
                }
            };

    @Override
    public Type<OpenPersonalAccessTerminalPacket> type() {
        return TYPE;
    }

    public static void handle(OpenPersonalAccessTerminalPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            PersonalAccessTerminalItem.openSatUi(player, packet.niPos(), packet.hubPos());
        });
    }
}
