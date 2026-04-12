package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: open the Backpack UI.
 *
 * <p>Sent when the player presses the backpack keybind. The server validates that the
 * indicated slot still holds a Backpack before opening the menu.
 *
 * <p>{@code slotType} is {@link #SLOT_INVENTORY} (vanilla inventory) or {@link #SLOT_CURIOS}
 * (Curios API slot). {@code slotId} is the Curios slot identifier (empty string for inventory).
 */
public record OpenBackpackPacket(int slotType, int slotIndex, String slotId) implements CustomPacketPayload {

    /** slotType value for a regular player inventory slot (indices 0-40). */
    public static final int SLOT_INVENTORY = 0;

    /** slotType value for a Curios slot. */
    public static final int SLOT_CURIOS = 1;

    public static final Type<OpenBackpackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "open_backpack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            OpenBackpackPacket::slotType,
            ByteBufCodecs.INT,
            OpenBackpackPacket::slotIndex,
            ByteBufCodecs.STRING_UTF8,
            OpenBackpackPacket::slotId,
            OpenBackpackPacket::new);

    @Override
    public Type<OpenBackpackPacket> type() {
        return TYPE;
    }

    public static void handle(OpenBackpackPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BackpackItem.openBackpackUi(player, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }
}
