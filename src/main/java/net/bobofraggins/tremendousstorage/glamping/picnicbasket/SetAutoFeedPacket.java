package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-bound packet: toggle the "auto-feed when hungry" setting on a Picnic Basket. */
public record SetAutoFeedPacket(BlockPos pos, boolean autoFeed) implements CustomPacketPayload {

    public static final Type<SetAutoFeedPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "set_auto_feed"));

    public static final StreamCodec<FriendlyByteBuf, SetAutoFeedPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetAutoFeedPacket::pos,
            ByteBufCodecs.BOOL,
            SetAutoFeedPacket::autoFeed,
            SetAutoFeedPacket::new);

    @Override
    public Type<SetAutoFeedPacket> type() {
        return TYPE;
    }

    public static void handle(SetAutoFeedPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof PicnicBasketBlockEntity pb) {
                pb.setAutoFeed(packet.autoFeed());
            } else if (be instanceof EnderPicnicBasketBlockEntity eb) {
                eb.setAutoFeed(packet.autoFeed());
            }
        });
    }
}
