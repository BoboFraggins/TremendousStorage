package net.bobofraggins.intellistore.shared.network;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.storage.tubeattachments.BreakerInterfaceScreen;
import net.bobofraggins.intellistore.storage.tubeattachments.ExportInterfaceScreen;
import net.bobofraggins.intellistore.storage.tubeattachments.ImportInterfaceScreen;
import net.bobofraggins.intellistore.storage.tubeattachments.PlacerInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-bound packet: sync the current filter state of an Import or Export Interface
 * attachment to all viewing clients after a server-side change.
 *
 * <p>Sent after {@link SetImportExportFilterPacket} is processed on the server.
 */
public record SyncInterfaceFilterPacket(BlockPos pos, int faceIndex, List<ItemStack> filterSlots, boolean rejectMode)
        implements CustomPacketPayload {

    public static final Type<SyncInterfaceFilterPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "sync_interface_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInterfaceFilterPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SyncInterfaceFilterPacket::pos,
                    ByteBufCodecs.INT,
                    SyncInterfaceFilterPacket::faceIndex,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)),
                    SyncInterfaceFilterPacket::filterSlots,
                    ByteBufCodecs.BOOL,
                    SyncInterfaceFilterPacket::rejectMode,
                    SyncInterfaceFilterPacket::new);

    @Override
    public Type<SyncInterfaceFilterPacket> type() {
        return TYPE;
    }

    public static void handle(SyncInterfaceFilterPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof ImportInterfaceScreen s
                    && s.getMenu().getPos().equals(packet.pos())
                    && s.getMenu().getFaceIndex() == packet.faceIndex()) {
                s.applySync(packet.filterSlots(), packet.rejectMode());
            } else if (current instanceof ExportInterfaceScreen s
                    && s.getMenu().getPos().equals(packet.pos())
                    && s.getMenu().getFaceIndex() == packet.faceIndex()) {
                s.applySync(packet.filterSlots(), packet.rejectMode());
            } else if (current instanceof PlacerInterfaceScreen s
                    && s.getMenu().getPos().equals(packet.pos())
                    && s.getMenu().getFaceIndex() == packet.faceIndex()) {
                ItemStack slot0 = packet.filterSlots().isEmpty()
                        ? ItemStack.EMPTY
                        : packet.filterSlots().get(0);
                s.applySync(slot0);
            } else if (current instanceof BreakerInterfaceScreen s
                    && s.getMenu().getPos().equals(packet.pos())
                    && s.getMenu().getFaceIndex() == packet.faceIndex()) {
                ItemStack slot0 = packet.filterSlots().isEmpty()
                        ? ItemStack.EMPTY
                        : packet.filterSlots().get(0);
                s.applySync(slot0);
            }
        });
    }

    /** Builds the filter slot list from a TubeBlockEntity's raw array. */
    public static List<ItemStack> buildSlotList(ItemStack[] slots) {
        List<ItemStack> list = new ArrayList<>(9);
        for (ItemStack s : slots) {
            list.add(s == null || s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1));
        }
        return list;
    }
}
