package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.tube.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-bound packet: update a single filter slot of an Import Interface or Export Interface
 * attachment on a Tube face.
 *
 * <p>{@code ghostItem} is stored in the given filter slot ({@code ItemStack.EMPTY} clears the slot).
 */
public record SetImportExportFilterPacket(BlockPos pos, int faceIndex, int slotIndex, ItemStack ghostItem)
        implements CustomPacketPayload {

    public static final Type<SetImportExportFilterPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "set_import_export_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetImportExportFilterPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetImportExportFilterPacket::pos,
                    ByteBufCodecs.INT,
                    SetImportExportFilterPacket::faceIndex,
                    ByteBufCodecs.INT,
                    SetImportExportFilterPacket::slotIndex,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SetImportExportFilterPacket::ghostItem,
                    SetImportExportFilterPacket::new);

    @Override
    public Type<SetImportExportFilterPacket> type() {
        return TYPE;
    }

    public static void handle(SetImportExportFilterPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level().getBlockEntity(packet.pos()) instanceof TubeBlockEntity be)) return;
            be.setFilterSlot(packet.faceIndex(), packet.slotIndex(), packet.ghostItem());
        });
    }
}
