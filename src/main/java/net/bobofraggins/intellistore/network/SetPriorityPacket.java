package net.bobofraggins.intellistore.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.bulkstorage.BulkStorageContainerBlockEntity;
import net.bobofraggins.intellistore.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.junkdrawer.JunkDrawerBlockEntity;
import net.bobofraggins.intellistore.priority.Priority;
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
 * Server-bound packet: set the priority of a storage block.
 *
 * <p>Valid for Filing Cabinet, Junk Drawer, and Bulk Storage Container. The handler
 * silently ignores unrecognised block entity types so mismatched packets cannot crash.
 */
public record SetPriorityPacket(BlockPos pos, int priority) implements CustomPacketPayload {

    public static final Type<SetPriorityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "set_priority"));

    public static final StreamCodec<FriendlyByteBuf, SetPriorityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetPriorityPacket::pos,
                    ByteBufCodecs.INT, SetPriorityPacket::priority,
                    SetPriorityPacket::new);

    @Override
    public Type<SetPriorityPacket> type() {
        return TYPE;
    }

    public static void handle(SetPriorityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            Priority p = Priority.fromOrdinal(packet.priority());
            if (be instanceof FilingCabinetBlockEntity fc) fc.setPriority(p);
            else if (be instanceof JunkDrawerBlockEntity jd) jd.setPriority(p);
            else if (be instanceof BulkStorageContainerBlockEntity bs) bs.setPriority(p);
        });
    }
}
