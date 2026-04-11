package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.config.SortMode;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackContents;
import net.bobofraggins.tremendousstorage.storage.backpack.BackpackItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server packet: set the sort mode of the Tremendous Backpack in the given slot. */
public record SetBackpackSortModePacket(int slotType, int slotIndex, String slotId, int modeOrdinal)
        implements CustomPacketPayload {

    public static final Type<SetBackpackSortModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "set_backpack_sort_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetBackpackSortModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SetBackpackSortModePacket::slotType,
                    ByteBufCodecs.INT,
                    SetBackpackSortModePacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    SetBackpackSortModePacket::slotId,
                    ByteBufCodecs.INT,
                    SetBackpackSortModePacket::modeOrdinal,
                    SetBackpackSortModePacket::new);

    @Override
    public Type<SetBackpackSortModePacket> type() {
        return TYPE;
    }

    public static void handle(SetBackpackSortModePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack backpackStack = BackpackItem.getBackpackStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;
            BackpackContents current = backpackStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
            SortMode newMode =
                    SortMode.values()[Math.max(0, Math.min(packet.modeOrdinal(), SortMode.values().length - 1))];
            backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), current.withSortMode(newMode));
            BackpackItem.setBackpackStack(
                    player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());
        });
    }
}
