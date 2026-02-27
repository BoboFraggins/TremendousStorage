package net.bobofraggins.intellistore.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: toggle the open/closed state of a Filing Cabinet.
 *
 * <p>Sent by the Filing Cabinet screen when the player clicks the Open/Closed button.
 */
public record ToggleFilingCabinetPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ToggleFilingCabinetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "toggle_filing_cabinet"));

    public static final StreamCodec<FriendlyByteBuf, ToggleFilingCabinetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ToggleFilingCabinetPacket::pos,
                    ToggleFilingCabinetPacket::new);

    @Override
    public Type<ToggleFilingCabinetPacket> type() {
        return TYPE;
    }

    public static void handle(ToggleFilingCabinetPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.level().getBlockEntity(packet.pos()) instanceof FilingCabinetBlockEntity be) {
                be.setOpen(!be.isOpen());
            }
        });
    }
}
