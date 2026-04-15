package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: insert or extract items in the Picnic Basket item-form UI.
 *
 * <p>Two operations:
 * <ul>
 *   <li><b>Insert</b> ({@code typeIndex == -1}): inserts the player's carried (cursor) item.
 *   <li><b>Extract</b> ({@code typeIndex >= 0}): extracts {@code amount} items at that grid index,
 *       placing them in the cursor ({@code toCursor = true}) or directly into inventory.
 * </ul>
 */
public record PicnicBasketItemInteractPacket(
        int slotType, int slotIndex, String slotId, int typeIndex, int amount, boolean toCursor)
        implements CustomPacketPayload {

    public static final Type<PicnicBasketItemInteractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "picnic_basket_item_interact"));

    public static final StreamCodec<FriendlyByteBuf, PicnicBasketItemInteractPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    PicnicBasketItemInteractPacket::slotType,
                    ByteBufCodecs.INT,
                    PicnicBasketItemInteractPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    PicnicBasketItemInteractPacket::slotId,
                    ByteBufCodecs.INT,
                    PicnicBasketItemInteractPacket::typeIndex,
                    ByteBufCodecs.INT,
                    PicnicBasketItemInteractPacket::amount,
                    ByteBufCodecs.BOOL,
                    PicnicBasketItemInteractPacket::toCursor,
                    PicnicBasketItemInteractPacket::new);

    @Override
    public Type<PicnicBasketItemInteractPacket> type() {
        return TYPE;
    }

    public static void handle(PicnicBasketItemInteractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack basketStack = PicnicBasketItemUtils.getBasketStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (basketStack.isEmpty()) return;
            HolderLookup.Provider registries = player.level().registryAccess();

            if (packet.typeIndex() == -1) {
                // Insert player's cursor item into the basket
                ItemStack carried = player.containerMenu.getCarried();
                if (carried.isEmpty()) return;
                long inserted = PicnicBasketItemUtils.insertIntoBasketItem(
                        registries, basketStack, carried, carried.getCount());
                PicnicBasketItemUtils.setBasketStack(
                        player, basketStack, packet.slotType(), packet.slotIndex(), packet.slotId());
                if (inserted > 0) {
                    long remaining = carried.getCount() - inserted;
                    player.containerMenu.setCarried(
                            remaining == 0 ? ItemStack.EMPTY : carried.copyWithCount((int) remaining));
                }
            } else {
                // Extract from basket grid
                if (packet.toCursor() && !player.containerMenu.getCarried().isEmpty()) return;
                ItemStack extracted = PicnicBasketItemUtils.extractFromBasketItem(
                        registries, basketStack, packet.typeIndex(), packet.amount());
                if (extracted.isEmpty()) return;
                PicnicBasketItemUtils.setBasketStack(
                        player, basketStack, packet.slotType(), packet.slotIndex(), packet.slotId());
                if (packet.toCursor()) {
                    player.containerMenu.setCarried(extracted);
                } else {
                    ItemHandlerHelper.giveItemToPlayer(player, extracted);
                }
            }
        });
    }
}
