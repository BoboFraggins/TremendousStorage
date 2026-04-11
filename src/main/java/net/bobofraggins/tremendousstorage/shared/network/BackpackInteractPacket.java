package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: insert or extract items from a Backpack's storage grid.
 *
 * <p>Two operations:
 * <ul>
 *   <li><b>Insert</b> ({@code typeIndex == -1}): inserts the player's carried (cursor) item.
 *   <li><b>Extract</b> ({@code typeIndex >= 0}): extracts {@code amount} items at that grid index,
 *       placing them in the cursor ({@code toCursor = true}) or directly into inventory.
 * </ul>
 */
public record BackpackInteractPacket(
        int slotType, int slotIndex, String slotId, int typeIndex, int amount, boolean toCursor)
        implements CustomPacketPayload {

    public static final Type<BackpackInteractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "backpack_interact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackInteractPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    BackpackInteractPacket::slotType,
                    ByteBufCodecs.INT,
                    BackpackInteractPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    BackpackInteractPacket::slotId,
                    ByteBufCodecs.INT,
                    BackpackInteractPacket::typeIndex,
                    ByteBufCodecs.INT,
                    BackpackInteractPacket::amount,
                    ByteBufCodecs.BOOL,
                    BackpackInteractPacket::toCursor,
                    BackpackInteractPacket::new);

    @Override
    public Type<BackpackInteractPacket> type() {
        return TYPE;
    }

    public static void handle(BackpackInteractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack backpackStack = BackpackItem.getBackpackStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;
            BackpackContents contents = backpackStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);

            if (packet.typeIndex() == -1) {
                // Insert player's cursor item into the backpack
                ItemStack carried = player.containerMenu.getCarried();
                if (carried.isEmpty()) return;
                Object[] result = contents.withInserted(carried, carried.getCount());
                long remainder = (long) result[0];
                BackpackContents updated = (BackpackContents) result[1];
                backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
                BackpackItem.setBackpackStack(
                        player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());
                int moved = (int) (carried.getCount() - remainder);
                if (moved > 0) {
                    ItemStack remainderStack =
                            remainder == 0 ? ItemStack.EMPTY : carried.copyWithCount((int) remainder);
                    player.containerMenu.setCarried(remainderStack);
                }
            } else {
                // Extract from backpack grid
                if (packet.toCursor() && !player.containerMenu.getCarried().isEmpty()) return;
                Object[] result = contents.withExtracted(packet.typeIndex(), packet.amount());
                ItemStack extracted = (ItemStack) result[0];
                BackpackContents updated = (BackpackContents) result[1];
                if (extracted.isEmpty()) return;
                backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
                BackpackItem.setBackpackStack(
                        player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());
                if (packet.toCursor()) {
                    player.containerMenu.setCarried(extracted);
                } else {
                    ItemHandlerHelper.giveItemToPlayer(player, extracted);
                }
            }
        });
    }
}
