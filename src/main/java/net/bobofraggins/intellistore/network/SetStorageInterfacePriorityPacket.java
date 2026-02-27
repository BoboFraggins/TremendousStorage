package net.bobofraggins.intellistore.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.priority.Priority;
import net.bobofraggins.intellistore.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: set the priority of a Storage Interface attachment on a Tube face.
 */
public record SetStorageInterfacePriorityPacket(BlockPos pos, int faceIndex, int priority)
        implements CustomPacketPayload {

    public static final Type<SetStorageInterfacePriorityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "set_storage_interface_priority"));

    public static final StreamCodec<FriendlyByteBuf, SetStorageInterfacePriorityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetStorageInterfacePriorityPacket::pos,
                    ByteBufCodecs.INT, SetStorageInterfacePriorityPacket::faceIndex,
                    ByteBufCodecs.INT, SetStorageInterfacePriorityPacket::priority,
                    SetStorageInterfacePriorityPacket::new);

    @Override
    public Type<SetStorageInterfacePriorityPacket> type() {
        return TYPE;
    }

    public static void handle(SetStorageInterfacePriorityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.level().getBlockEntity(packet.pos()) instanceof TubeBlockEntity be) {
                be.setAttachmentPriority(packet.faceIndex(), Priority.fromOrdinal(packet.priority()));
            }
        });
    }
}
