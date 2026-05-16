package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackItem;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.FannyPackContents;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server packet: toggle void excess on the Dank Fanny Pack in the given slot. */
public record SetFannyPackVoidExcessPacket(int slotType, int slotIndex, String slotId, boolean voidExcess)
        implements CustomPacketPayload {

    public static final Type<SetFannyPackVoidExcessPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "set_fanny_pack_void_excess"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFannyPackVoidExcessPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SetFannyPackVoidExcessPacket::slotType,
                    ByteBufCodecs.INT,
                    SetFannyPackVoidExcessPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    SetFannyPackVoidExcessPacket::slotId,
                    ByteBufCodecs.BOOL,
                    SetFannyPackVoidExcessPacket::voidExcess,
                    SetFannyPackVoidExcessPacket::new);

    @Override
    public Type<SetFannyPackVoidExcessPacket> type() {
        return TYPE;
    }

    public static void handle(SetFannyPackVoidExcessPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack stack =
                    DankFannyPackItem.getFannyPackStack(player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (stack.isEmpty()) return;
            FannyPackContents current =
                    stack.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);
            stack.set(Registration.FANNY_PACK_CONTENTS.get(), current.withVoidExcess(packet.voidExcess()));
            DankFannyPackItem.setFannyPackStack(player, stack, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }
}
