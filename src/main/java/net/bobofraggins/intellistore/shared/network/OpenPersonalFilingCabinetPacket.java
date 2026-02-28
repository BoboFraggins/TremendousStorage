package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: open the Personal Filing Cabinet UI.
 *
 * <p>The client sends the slot location so the server can open the correct PFC instance.
 * {@code slotType} is 0 for regular inventory (slotIndex = vanilla slot, 0–40),
 * or 1 for a Curios slot (slotIndex = slot within the curios handler, slotId = curios slot name).
 */
public record OpenPersonalFilingCabinetPacket(int slotType, int slotIndex, String slotId)
        implements CustomPacketPayload {

    /** slotType value for a regular player inventory slot. */
    public static final int SLOT_INVENTORY = 0;
    /** slotType value for a Curios slot. */
    public static final int SLOT_CURIOS = 1;

    public static final Type<OpenPersonalFilingCabinetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "open_personal_filing_cabinet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPersonalFilingCabinetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    OpenPersonalFilingCabinetPacket::slotType,
                    ByteBufCodecs.INT,
                    OpenPersonalFilingCabinetPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    OpenPersonalFilingCabinetPacket::slotId,
                    OpenPersonalFilingCabinetPacket::new);

    @Override
    public Type<OpenPersonalFilingCabinetPacket> type() {
        return TYPE;
    }

    public static void handle(OpenPersonalFilingCabinetPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            PersonalFilingCabinetItem.openPfcUi(player, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }
}
