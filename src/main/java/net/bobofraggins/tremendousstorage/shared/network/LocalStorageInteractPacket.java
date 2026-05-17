package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet for direct player interaction with a local storage block.
 *
 * <p>Handles two operations:
 * <ul>
 *   <li><b>Insert</b> ({@code typeIndex == -1}): inserts the player's carried (cursor) item into
 *       the container; the cursor is updated with any remainder.
 *   <li><b>Extract</b> ({@code typeIndex >= 0}): extracts {@code amount} items at {@code typeIndex}
 *       from the container and places them in the player's cursor ({@code toCursor = true}) or
 *       directly into the player's inventory ({@code toCursor = false}).
 * </ul>
 */
public record LocalStorageInteractPacket(BlockPos pos, boolean isBulk, int typeIndex, int amount, boolean toCursor)
        implements CustomPacketPayload {

    public static final Type<LocalStorageInteractPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "local_storage_interact"));

    public static final StreamCodec<FriendlyByteBuf, LocalStorageInteractPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LocalStorageInteractPacket::pos,
            StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean),
            LocalStorageInteractPacket::isBulk,
            StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
            LocalStorageInteractPacket::typeIndex,
            StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
            LocalStorageInteractPacket::amount,
            StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean),
            LocalStorageInteractPacket::toCursor,
            LocalStorageInteractPacket::new);

    @Override
    public Type<LocalStorageInteractPacket> type() {
        return TYPE;
    }

    public static void handle(LocalStorageInteractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());

            if (packet.typeIndex() == -1) {
                // Insert player's carried item into the container
                ItemStack carried = player.containerMenu.getCarried();
                if (carried.isEmpty()) return;

                ItemStack remainder;
                if (!(be instanceof ChestBlockEntity bulk)) return;
                long left = bulk.insert(carried, carried.getCount(), false);
                remainder = left == 0 ? ItemStack.EMPTY : carried.copyWithCount((int) left);
                player.containerMenu.setCarried(remainder);

            } else {
                // Extract from container
                if (packet.toCursor() && !player.containerMenu.getCarried().isEmpty()) return;

                ItemStack result;
                if (!(be instanceof ChestBlockEntity bulk)) return;
                result = bulk.extract(packet.typeIndex(), packet.amount(), false);

                if (!result.isEmpty()) {
                    if (packet.toCursor()) {
                        player.containerMenu.setCarried(result);
                    } else {
                        if (!player.addItem(result)) player.drop(result, false);
                    }
                }
            }
        });
    }
}
