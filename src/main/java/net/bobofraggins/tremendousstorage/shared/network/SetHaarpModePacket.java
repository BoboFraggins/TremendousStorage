package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WeatherMode;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-bound packet: change the HAARP weather mode on a Wireless Hub. */
public record SetHaarpModePacket(BlockPos pos, int modeOrdinal) implements CustomPacketPayload {

    public static final Type<SetHaarpModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "set_haarp_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetHaarpModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetHaarpModePacket::pos,
            ByteBufCodecs.INT,
            SetHaarpModePacket::modeOrdinal,
            SetHaarpModePacket::new);

    @Override
    public Type<SetHaarpModePacket> type() {
        return TYPE;
    }

    public static void handle(SetHaarpModePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level().getBlockEntity(packet.pos()) instanceof WirelessHubBlockEntity be)) return;
            be.setHaarpMode(WeatherMode.fromOrdinal(packet.modeOrdinal()));
        });
    }
}
