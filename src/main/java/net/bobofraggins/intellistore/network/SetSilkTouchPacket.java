package net.bobofraggins.intellistore.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: set the Silk Touch flag on a Breaker Interface attachment.
 */
public record SetSilkTouchPacket(BlockPos pos, int faceIndex, boolean silkTouch)
        implements CustomPacketPayload {

    public static final Type<SetSilkTouchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "set_silk_touch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSilkTouchPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetSilkTouchPacket::pos,
                    ByteBufCodecs.INT,
                    SetSilkTouchPacket::faceIndex,
                    ByteBufCodecs.BOOL,
                    SetSilkTouchPacket::silkTouch,
                    SetSilkTouchPacket::new);

    @Override
    public Type<SetSilkTouchPacket> type() {
        return TYPE;
    }

    public static void handle(SetSilkTouchPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level().getBlockEntity(packet.pos()) instanceof TubeBlockEntity be)) return;
            be.setSilkTouch(packet.faceIndex(), packet.silkTouch());
        });
    }
}
