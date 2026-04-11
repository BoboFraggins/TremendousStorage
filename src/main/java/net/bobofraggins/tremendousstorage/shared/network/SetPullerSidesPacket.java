package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
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
 * Server-bound packet: toggle which sides the Puller Upgrade pulls from.
 *
 * <p>Valid for Tremendous Chest and Filing Cabinet. The {@code sidesMask} is a 6-bit int
 * where bit 0=TOP, 1=BOTTOM, 2=LEFT, 3=RIGHT, 4=FRONT, 5=BACK.
 */
public record SetPullerSidesPacket(BlockPos pos, int sidesMask) implements CustomPacketPayload {

    public static final Type<SetPullerSidesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "set_puller_sides"));

    public static final StreamCodec<FriendlyByteBuf, SetPullerSidesPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetPullerSidesPacket::pos,
            ByteBufCodecs.INT,
            SetPullerSidesPacket::sidesMask,
            SetPullerSidesPacket::new);

    @Override
    public Type<SetPullerSidesPacket> type() {
        return TYPE;
    }

    public static void handle(SetPullerSidesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof ChestBlockEntity chest) chest.setPullerSides(packet.sidesMask());
            else if (be instanceof FilingCabinetBlockEntity fc) fc.setPullerSides(packet.sidesMask());
        });
    }
}
