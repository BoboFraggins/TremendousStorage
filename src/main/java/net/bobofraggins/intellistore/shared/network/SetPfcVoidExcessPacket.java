package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetContents;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetItem;
import net.bobofraggins.intellistore.shared.register.Registration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: toggle the Void Excess flag on a Personal Filing Cabinet item.
 */
public record SetPfcVoidExcessPacket(int pfcSlot, boolean voidExcess)
        implements CustomPacketPayload {

    public static final Type<SetPfcVoidExcessPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "set_pfc_void_excess"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPfcVoidExcessPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SetPfcVoidExcessPacket::pfcSlot,
                    ByteBufCodecs.BOOL,
                    SetPfcVoidExcessPacket::voidExcess,
                    SetPfcVoidExcessPacket::new);

    @Override
    public Type<SetPfcVoidExcessPacket> type() {
        return TYPE;
    }

    public static void handle(SetPfcVoidExcessPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack pfcStack = player.getInventory().getItem(packet.pfcSlot());
            if (!(pfcStack.getItem() instanceof PersonalFilingCabinetItem)) return;

            PersonalFilingCabinetContents contents =
                    pfcStack.getOrDefault(Registration.PFC_CONTENTS.get(), PersonalFilingCabinetContents.EMPTY);

            // Preserve existing folders, just update voidExcess
            List<ItemStack> folders = new ArrayList<>(contents.folders());
            pfcStack.set(Registration.PFC_CONTENTS.get(),
                    new PersonalFilingCabinetContents(List.copyOf(folders), packet.voidExcess()));
            player.getInventory().setItem(packet.pfcSlot(), pfcStack);
        });
    }
}
