package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: extract items of a specific type from the network.
 *
 * <p>Carries the NI position directly so the handler can skip BFS lookup.
 * Extracts up to {@code amount} items matching {@code target} (by item type + components)
 * from the network via the NI's item handler, and places them in the player's inventory.
 * Sends back an updated {@link SatContentsPacket} after the operation.
 */
public record SatExtractPacket(BlockPos niPos, ItemStack target, int amount) implements CustomPacketPayload {

    public static final Type<SatExtractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "sat_extract"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> VAR_INT_CODEC =
            StreamCodec.of((buf, v) -> buf.writeVarInt(v), RegistryFriendlyByteBuf::readVarInt);

    public static final StreamCodec<RegistryFriendlyByteBuf, SatExtractPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.cast(),
            SatExtractPacket::niPos,
            ItemStack.OPTIONAL_STREAM_CODEC,
            SatExtractPacket::target,
            VAR_INT_CODEC,
            SatExtractPacket::amount,
            SatExtractPacket::new);

    @Override
    public Type<SatExtractPacket> type() {
        return TYPE;
    }

    public static void handle(SatExtractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            BlockEntity be = player.level().getBlockEntity(packet.niPos());
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) return;

            IItemHandler handler = ni.getItemHandler();
            if (handler == null) return;

            // Extract up to `amount` items matching target type+components
            int remaining = packet.amount();
            for (int s = 0; s < handler.getSlots() && remaining > 0; s++) {
                ItemStack inSlot = handler.getStackInSlot(s);
                if (inSlot.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(inSlot, packet.target())) continue;

                int toExtract = Math.min(remaining, inSlot.getCount());
                ItemStack extracted = handler.extractItem(s, toExtract, false);
                if (extracted.isEmpty()) continue;

                remaining -= extracted.getCount();
                // Give extracted items to player (respects inventory rules)
                ItemHandlerHelper.giveItemToPlayer(player, extracted);
            }

            // Refresh client's item list
            IItemHandler refreshedHandler = ni.getItemHandler();
            if (refreshedHandler != null) {
                PacketDistributor.sendToPlayer(player, RequestSatContentsPacket.buildContentsPacket(refreshedHandler));
            }
        });
    }
}
