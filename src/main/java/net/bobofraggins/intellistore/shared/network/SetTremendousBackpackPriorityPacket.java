package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackContents;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackItem;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server packet: set the priority of the Tremendous Backpack in the given slot. */
public record SetTremendousBackpackPriorityPacket(int slotType, int slotIndex, String slotId, int priority)
        implements CustomPacketPayload {

    public static final Type<SetTremendousBackpackPriorityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "set_backpack_priority"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetTremendousBackpackPriorityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SetTremendousBackpackPriorityPacket::slotType,
                    ByteBufCodecs.INT,
                    SetTremendousBackpackPriorityPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    SetTremendousBackpackPriorityPacket::slotId,
                    ByteBufCodecs.INT,
                    SetTremendousBackpackPriorityPacket::priority,
                    SetTremendousBackpackPriorityPacket::new);

    @Override
    public Type<SetTremendousBackpackPriorityPacket> type() {
        return TYPE;
    }

    public static void handle(SetTremendousBackpackPriorityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack backpackStack = TremendousBackpackItem.getBackpackStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;
            TremendousBackpackContents current = backpackStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);
            Priority newPriority = Priority.fromOrdinal(packet.priority());
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withPriority(newPriority));
            TremendousBackpackItem.setBackpackStack(
                    player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());
            // Propagate to the open menu's ContainerData so the client syncs immediately
            if (player.containerMenu instanceof TremendousBackpackMenu menu) {
                menu.setPriorityInData(packet.priority());
            }
        });
    }
}
