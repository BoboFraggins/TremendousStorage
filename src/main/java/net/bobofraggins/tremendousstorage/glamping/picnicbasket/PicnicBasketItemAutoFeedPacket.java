package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-bound packet: toggle the auto-feed setting on a Picnic Basket held in the player's inventory. */
public record PicnicBasketItemAutoFeedPacket(int slotType, int slotIndex, String slotId, boolean autoFeed)
        implements CustomPacketPayload {

    public static final Type<PicnicBasketItemAutoFeedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "picnic_basket_item_auto_feed"));

    public static final StreamCodec<FriendlyByteBuf, PicnicBasketItemAutoFeedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    PicnicBasketItemAutoFeedPacket::slotType,
                    ByteBufCodecs.INT,
                    PicnicBasketItemAutoFeedPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    PicnicBasketItemAutoFeedPacket::slotId,
                    ByteBufCodecs.BOOL,
                    PicnicBasketItemAutoFeedPacket::autoFeed,
                    PicnicBasketItemAutoFeedPacket::new);

    @Override
    public Type<PicnicBasketItemAutoFeedPacket> type() {
        return TYPE;
    }

    public static void handle(PicnicBasketItemAutoFeedPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack basketStack = PicnicBasketItemUtils.getBasketStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (basketStack.isEmpty()) return;
            PicnicBasketItemUtils.writeAutoFeed(basketStack, packet.autoFeed());
            PicnicBasketItemUtils.setBasketStack(
                    player, basketStack, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }
}
