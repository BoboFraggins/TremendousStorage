package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackItem;
import net.bobofraggins.tremendousstorage.glamping.dankfannypack.DankFannyPackMenu;
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

/** Client-to-server packet: set the priority of the Dank Fanny Pack in the given slot. */
public record SetFannyPackPriorityPacket(int slotType, int slotIndex, String slotId, int priority)
        implements CustomPacketPayload {

    public static final Type<SetFannyPackPriorityPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "set_fanny_pack_priority"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFannyPackPriorityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SetFannyPackPriorityPacket::slotType,
                    ByteBufCodecs.INT,
                    SetFannyPackPriorityPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    SetFannyPackPriorityPacket::slotId,
                    ByteBufCodecs.INT,
                    SetFannyPackPriorityPacket::priority,
                    SetFannyPackPriorityPacket::new);

    @Override
    public Type<SetFannyPackPriorityPacket> type() {
        return TYPE;
    }

    public static void handle(SetFannyPackPriorityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack stack =
                    DankFannyPackItem.getFannyPackStack(player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (stack.isEmpty()) return;
            FannyPackContents current =
                    stack.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);
            stack.set(Registration.FANNY_PACK_CONTENTS.get(), current.withPriority(packet.priority()));
            DankFannyPackItem.setFannyPackStack(player, stack, packet.slotType(), packet.slotIndex(), packet.slotId());
            // Propagate to open menu so client ContainerData syncs immediately
            if (player.containerMenu instanceof DankFannyPackMenu menu) {
                menu.setPriorityInData(packet.priority());
            }
        });
    }
}
