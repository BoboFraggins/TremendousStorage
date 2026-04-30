package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: open the Dank Fanny Pack UI.
 *
 * <p>Sent when the player presses the fanny pack keybind or right-clicks the item. The server
 * validates that the indicated slot still holds a Dank Fanny Pack before opening the menu.
 */
public record OpenFannyPackPacket(int slotType, int slotIndex, String slotId) implements CustomPacketPayload {

    public static final int SLOT_INVENTORY = 0;
    public static final int SLOT_CURIOS = 1;

    public static final Type<OpenFannyPackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "open_fanny_pack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFannyPackPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            OpenFannyPackPacket::slotType,
            ByteBufCodecs.INT,
            OpenFannyPackPacket::slotIndex,
            ByteBufCodecs.STRING_UTF8,
            OpenFannyPackPacket::slotId,
            OpenFannyPackPacket::new);

    @Override
    public Type<OpenFannyPackPacket> type() {
        return TYPE;
    }

    public static void handle(OpenFannyPackPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            DankFannyPackItem.openFannyPackUi(player, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }
}
