package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server packet: set the priority of the Tremendous Backpack in the given slot. */
public record SetBackpackPriorityPacket(int slotType, int slotIndex, String slotId, int priority)
        implements CustomPacketPayload {

    public static final Type<SetBackpackPriorityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "set_backpack_priority"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetBackpackPriorityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SetBackpackPriorityPacket::slotType,
                    ByteBufCodecs.INT,
                    SetBackpackPriorityPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    SetBackpackPriorityPacket::slotId,
                    ByteBufCodecs.INT,
                    SetBackpackPriorityPacket::priority,
                    SetBackpackPriorityPacket::new);

    @Override
    public Type<SetBackpackPriorityPacket> type() {
        return TYPE;
    }

    public static void handle(SetBackpackPriorityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack backpackStack = BackpackItem.getBackpackStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;
            BackpackContents current = backpackStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
            Priority newPriority = Priority.fromOrdinal(packet.priority());
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withPriority(newPriority));
            BackpackItem.setBackpackStack(
                    player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());
            // Propagate to the open menu's ContainerData so the client syncs immediately
            if (player.containerMenu instanceof BackpackMenu menu) {
                menu.setPriorityInData(packet.priority());
            }
        });
    }
}
