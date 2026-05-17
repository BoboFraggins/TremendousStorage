package net.bobofraggins.tremendousstorage.shared.network;

import java.util.Set;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.storage.KeyCounter;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Client-to-server packet: extract items of a specific type from the network.
 *
 * <p>Carries the NI position directly so the handler can skip BFS lookup.
 * Extracts up to {@code amount} items matching {@code target} (by item type + components)
 * from the network via the NI's item handler, and places them in the player's inventory.
 * Sends back an updated {@link SatContentsPacket} after the operation.
 *
 * <p>For fluid-backed slots (detected via {@link NiItemHandler.SlotView#isFluid}), an empty bucket
 * must be present in the player's cursor; one empty bucket is consumed per filled bucket
 * extracted.
 */
public record SatExtractPacket(BlockPos niPos, ItemStack target, int amount, boolean toCursor)
        implements CustomPacketPayload {

    public static final Type<SatExtractPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "sat_extract"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> VAR_INT_CODEC =
            StreamCodec.of((buf, v) -> buf.writeVarInt(v), RegistryFriendlyByteBuf::readVarInt);

    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOL_CODEC =
            StreamCodec.of((buf, b) -> buf.writeBoolean(b), buf -> buf.readBoolean());

    public static final StreamCodec<RegistryFriendlyByteBuf, SatExtractPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.cast(),
            SatExtractPacket::niPos,
            ItemStack.OPTIONAL_STREAM_CODEC,
            SatExtractPacket::target,
            VAR_INT_CODEC,
            SatExtractPacket::amount,
            BOOL_CODEC,
            SatExtractPacket::toCursor,
            SatExtractPacket::new);

    @Override
    public Type<SatExtractPacket> type() {
        return TYPE;
    }

    public static void handle(SatExtractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            BlockEntity be = player.level().getBlockEntity(packet.niPos());
            if (!(be instanceof NetworkInterfaceBlockEntity ni)) return;

            ResourceHandler<ItemResource> handler = ni.getItemHandler();
            if (handler == null) return;

            ItemStack carried = player.containerMenu.getCarried();
            int remaining = packet.amount();
            ItemStack result = ItemStack.EMPTY;
            boolean anyFluidExtracted = false;
            ItemResource targetResource = ItemResource.of(packet.target());

            // Use direct slot iteration when available to avoid O(H) resolveSlot per slot.
            Iterable<?> slotSource = handler instanceof NiItemHandler nhi ? nhi.slots() : null;

            if (slotSource != null) {
                for (Object obj : slotSource) {
                    if (remaining <= 0) break;
                    NiItemHandler.SlotView view = (NiItemHandler.SlotView) obj;
                    ItemResource res = view.handler().getResource(view.localSlot());
                    if (res.isEmpty()) continue;

                    if (view.isFluid()) {
                        if (!res.equals(targetResource)) continue;
                        if (carried.isEmpty() || !carried.is(Items.BUCKET)) break;
                        int toExtract = Math.min(remaining, carried.getCount());
                        int extracted = view.handler().extract(view.localSlot(), res, toExtract, null);
                        if (extracted <= 0) continue;
                        carried.shrink(extracted);
                        if (carried.isEmpty()) player.containerMenu.setCarried(ItemStack.EMPTY);
                        else player.containerMenu.setCarried(carried);
                        remaining -= extracted;
                        if (result.isEmpty()) result = res.toStack(extracted);
                        else result.grow(extracted);
                        anyFluidExtracted = true;
                    } else {
                        if (!res.equals(targetResource)) continue;
                        long available = view.handler().getAmountAsLong(view.localSlot());
                        int toExtract = (int) Math.min(remaining, available);
                        int extracted = view.handler().extract(view.localSlot(), res, toExtract, null);
                        if (extracted <= 0) continue;
                        remaining -= extracted;
                        if (result.isEmpty()) result = res.toStack(extracted);
                        else result.grow(extracted);
                    }
                }
            } else {
                for (int s = 0; s < handler.size() && remaining > 0; s++) {
                    ItemResource res = handler.getResource(s);
                    if (res.isEmpty() || !res.equals(targetResource)) continue;
                    int toExtract = (int) Math.min(remaining, handler.getAmountAsLong(s));
                    int extracted = handler.extract(s, res, toExtract, null);
                    if (extracted <= 0) continue;
                    remaining -= extracted;
                    if (result.isEmpty()) result = res.toStack(extracted);
                    else result.grow(extracted);
                }
            }

            // Route to cursor or directly to inventory
            if (!result.isEmpty()) {
                if (packet.toCursor() && player.containerMenu.getCarried().isEmpty()) {
                    player.containerMenu.setCarried(result);
                } else if (!player.addItem(result)) {
                    player.drop(result, false);
                }
            }

            // Play bucket-fill sound when fluid was extracted via an empty bucket
            if (anyFluidExtracted && !result.isEmpty()) {
                FluidUtil.getFluidContained(result).ifPresent(fluidStack -> {
                    SoundEvent sound = fluidStack.getFluidType().getSound(SoundActions.BUCKET_FILL);
                    if (sound == null) sound = SoundEvents.BUCKET_FILL;
                    player.level()
                            .playSound(
                                    null,
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    sound,
                                    SoundSource.BLOCKS,
                                    1.0f,
                                    1.0f);
                });
            }

            // Refresh client's item list (using KeyCounter path to include fluid tagging)
            KeyCounter refreshed = ni.getCachedInventory();
            if (refreshed != null) {
                Set<StorageKey> fluidKeys = ni.getFluidStorageKeys();
                PacketDistributor.sendToPlayer(
                        player, RequestSatContentsPacket.buildContentsPacket(refreshed, fluidKeys));
            }
        });
    }
}
