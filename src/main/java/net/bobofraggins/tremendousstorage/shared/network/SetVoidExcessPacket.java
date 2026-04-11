package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: toggle the "void excess" setting on a storage block.
 *
 * <p>Valid for Filing Cabinet and Tremendous Tank.
 */
public record SetVoidExcessPacket(BlockPos pos, boolean voidExcess) implements CustomPacketPayload {

    public static final Type<SetVoidExcessPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "set_void_excess"));

    public static final StreamCodec<FriendlyByteBuf, SetVoidExcessPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetVoidExcessPacket::pos,
            ByteBufCodecs.BOOL,
            SetVoidExcessPacket::voidExcess,
            SetVoidExcessPacket::new);

    @Override
    public Type<SetVoidExcessPacket> type() {
        return TYPE;
    }

    public static void handle(SetVoidExcessPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof FilingCabinetBlockEntity fc) {
                fc.setVoidExcess(packet.voidExcess());
            } else if (be instanceof TankBlockEntity ft) {
                ft.setVoidExcess(packet.voidExcess());
            }
        });
    }
}
