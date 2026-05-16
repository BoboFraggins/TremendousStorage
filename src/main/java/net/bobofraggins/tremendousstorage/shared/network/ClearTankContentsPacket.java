package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: clear and unlock the Tremendous Tank at the given position.
 *
 * <p>Calls {@link TankBlockEntity#clearFluid()}, which sets the amount to zero and removes
 * the fluid-type lock. Sent by the "Clear Contents" button in the tank settings screen.
 */
public record ClearTankContentsPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ClearTankContentsPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "clear_tank_contents"));

    public static final StreamCodec<FriendlyByteBuf, ClearTankContentsPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ClearTankContentsPacket::pos, ClearTankContentsPacket::new);

    @Override
    public Type<ClearTankContentsPacket> type() {
        return TYPE;
    }

    public static void handle(ClearTankContentsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof TankBlockEntity tank) {
                tank.clearFluid();
            }
        });
    }
}
