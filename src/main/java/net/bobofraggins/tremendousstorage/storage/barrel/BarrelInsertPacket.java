package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: insert the player's main-hand item into the barrel at {@code pos}.
 * Sent when the player left-clicks the item-display area on the barrel's front face.
 */
public record BarrelInsertPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<BarrelInsertPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "barrel_insert"));

    public static final StreamCodec<FriendlyByteBuf, BarrelInsertPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, BarrelInsertPacket::pos, BarrelInsertPacket::new);

    @Override
    public Type<BarrelInsertPacket> type() {
        return TYPE;
    }

    public static void handle(BarrelInsertPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level().getBlockEntity(packet.pos()) instanceof BarrelBlockEntity be)) return;
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) return;
            if (be.isLocked() && !ItemStack.isSameItemSameComponents(be.getStoredItem(), held)) return;
            long remainder = be.insert(held, held.getCount(), false);
            held.shrink(held.getCount() - (int) remainder);
        });
    }
}
