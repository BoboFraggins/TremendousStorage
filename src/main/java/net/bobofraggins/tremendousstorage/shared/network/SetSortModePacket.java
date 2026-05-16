package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: set the sort mode of a storage block.
 *
 * <p>Valid for the Storage Access Terminal and Tremendous Chest.
 */
public record SetSortModePacket(BlockPos pos, int modeOrdinal) implements CustomPacketPayload {

    public static final Type<SetSortModePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "set_sort_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetSortModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetSortModePacket::pos,
            ByteBufCodecs.INT,
            SetSortModePacket::modeOrdinal,
            SetSortModePacket::new);

    public SetSortModePacket(BlockPos pos, SortMode mode) {
        this(pos, mode.ordinal());
    }

    @Override
    public Type<SetSortModePacket> type() {
        return TYPE;
    }

    public static void handle(SetSortModePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            SortMode mode =
                    SortMode.values()[Math.max(0, Math.min(packet.modeOrdinal(), SortMode.values().length - 1))];
            if (be instanceof AccessTerminalBlockEntity at) at.setSortMode(mode);
            else if (be instanceof ChestBlockEntity bs) bs.setSortMode(mode);
        });
    }
}
