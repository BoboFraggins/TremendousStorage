package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.intellistore.storage.fluidtank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: toggle the "void excess" setting on a storage block.
 *
 * <p>Valid for Filing Cabinet, Fluid Tank, Gas Tank, and Source Tank.
 * Gas Tank and Source Tank are referenced by class name only when their mods are present;
 * the instanceof checks are safe because those classes are never loaded otherwise.
 */
public record SetVoidExcessPacket(BlockPos pos, boolean voidExcess) implements CustomPacketPayload {

    public static final Type<SetVoidExcessPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "set_void_excess"));

    public static final StreamCodec<FriendlyByteBuf, SetVoidExcessPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetVoidExcessPacket::pos,
            ByteBufCodecs.BOOL,
            SetVoidExcessPacket::voidExcess,
            SetVoidExcessPacket::new);

    @Override
    public Type<SetVoidExcessPacket> type() {
        return TYPE;
    }

    public static void handle(SetVoidExcessPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof FilingCabinetBlockEntity fc) {
                fc.setVoidExcess(packet.voidExcess());
            } else if (be instanceof FluidTankBlockEntity ft) {
                ft.setVoidExcess(packet.voidExcess());
            } else {
                handleOptional(be, packet.voidExcess());
            }
        });
    }

    /**
     * Handles Gas Tank and Source Tank without importing their classes at the top level,
     * keeping the optional-mod classes safely isolated.
     */
    private static void handleOptional(BlockEntity be, boolean value) {
        if (net.neoforged.fml.ModList.get().isLoaded("mekanism")
                && be instanceof net.bobofraggins.intellistore.external.mekanism.GasTankBlockEntity gt) {
            gt.setVoidExcess(value);
        } else if (net.neoforged.fml.ModList.get().isLoaded("ars_nouveau")
                && be instanceof net.bobofraggins.intellistore.external.arsnouveau.SourceTankBlockEntity st) {
            st.setVoidExcess(value);
        }
    }
}
