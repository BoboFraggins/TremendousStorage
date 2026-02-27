package net.bobofraggins.intellistore.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: insert a player inventory slot's stack into the network.
 *
 * <p>Takes the stack at {@code playerSlot} in the player's inventory and inserts as
 * much as possible into the network via the NI item handler. Any remainder stays
 * in the inventory slot. Sends back an updated {@link SatContentsPacket} after.
 */
public record SatInsertPacket(BlockPos niPos, int playerSlot) implements CustomPacketPayload {

    public static final Type<SatInsertPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "sat_insert"));

    public static final StreamCodec<FriendlyByteBuf, SatInsertPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SatInsertPacket::niPos,
            StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
            SatInsertPacket::playerSlot,
            SatInsertPacket::new);

    @Override
    public Type<SatInsertPacket> type() {
        return TYPE;
    }

    public static void handle(SatInsertPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            // Validate slot index
            int slot = packet.playerSlot();
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;

            ItemStack inSlot = player.getInventory().getItem(slot);
            if (inSlot.isEmpty()) return;

            BlockEntity be = player.level().getBlockEntity(packet.niPos());
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) return;

            IItemHandler handler = ni.getItemHandler();
            if (handler == null) return;

            // Insert the stack into the network; remainder stays in the slot
            ItemStack toInsert = inSlot.copy();
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            // insertItem slot=0 is ignored by NiItemHandler (it iterates all handlers)
            // Set remaining amount back in player slot
            player.getInventory().setItem(slot, remainder);
            player.getInventory().setChanged();

            // Refresh client's item list
            IItemHandler refreshedHandler = ni.getItemHandler();
            if (refreshedHandler != null) {
                PacketDistributor.sendToPlayer(player, RequestSatContentsPacket.buildContentsPacket(refreshedHandler));
            }
        });
    }
}
