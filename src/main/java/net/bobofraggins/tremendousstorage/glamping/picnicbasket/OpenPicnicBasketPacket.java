package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: open the Picnic Basket item-form UI.
 *
 * <p>Sent when the player presses the picnic basket keybind. The server validates that the
 * indicated slot still holds a picnic basket before opening the menu.
 */
public record OpenPicnicBasketPacket(int slotType, int slotIndex, String slotId) implements CustomPacketPayload {

    public static final Type<OpenPicnicBasketPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "open_picnic_basket"));

    public static final StreamCodec<FriendlyByteBuf, OpenPicnicBasketPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            OpenPicnicBasketPacket::slotType,
            ByteBufCodecs.INT,
            OpenPicnicBasketPacket::slotIndex,
            ByteBufCodecs.STRING_UTF8,
            OpenPicnicBasketPacket::slotId,
            OpenPicnicBasketPacket::new);

    @Override
    public Type<OpenPicnicBasketPacket> type() {
        return TYPE;
    }

    public static void handle(OpenPicnicBasketPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            openUi(player, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }

    public static void openUi(ServerPlayer player, int slotType, int slotIndex, String slotId) {
        ItemStack basketStack = PicnicBasketItemUtils.getBasketStack(player, slotType, slotIndex, slotId);
        if (basketStack.isEmpty()) return;
        player.openMenu(new Provider(slotType, slotIndex, slotId), buf -> {
            buf.writeInt(slotType);
            buf.writeInt(slotIndex);
            buf.writeUtf(slotId);
        });
    }

    private static class Provider implements MenuProvider {
        private final int slotType;
        private final int slotIndex;
        private final String slotId;

        Provider(int slotType, int slotIndex, String slotId) {
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.slotId = slotId;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.tremendousstorage.picnic_basket");
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
            return new PicnicBasketItemMenu(syncId, inv, slotType, slotIndex, slotId);
        }
    }
}
