package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
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
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "sat_insert"));

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

            int slot = packet.playerSlot();
            ItemStack inSlot;

            if (slot == -1) {
                // Cursor insert: take item from the player's open menu cursor
                if (player.containerMenu == null) return;
                inSlot = player.containerMenu.getCarried();
                if (inSlot.isEmpty()) return;
            } else {
                if (slot >= player.getInventory().getContainerSize()) return;
                inSlot = player.getInventory().getItem(slot);
                if (inSlot.isEmpty()) return;
            }

            BlockEntity be = player.level().getBlockEntity(packet.niPos());
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) return;

            IItemHandler handler = ni.getItemHandler();
            if (handler == null) return;

            // Insert the stack into the network; remainder goes back to source
            ItemStack toInsert = inSlot.copy();
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            if (slot == -1) {
                player.containerMenu.setCarried(remainder);
            } else {
                player.getInventory().setItem(slot, remainder);
                player.getInventory().setChanged();
            }

            // Refresh client's item list
            IItemHandler refreshedHandler = ni.getItemHandler();
            if (refreshedHandler != null) {
                PacketDistributor.sendToPlayer(player, RequestSatContentsPacket.buildContentsPacket(refreshedHandler));
            }
        });
    }
}
