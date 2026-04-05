package net.bobofraggins.intellistore.shared.network;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackContents;
import net.bobofraggins.intellistore.storage.tremendousbackpack.TremendousBackpackItem;
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
 * Client-to-server packet: insert or extract items from a Tremendous Backpack's storage grid.
 *
 * <p>Two operations:
 * <ul>
 *   <li><b>Insert</b> ({@code typeIndex == -1}): inserts the player's carried (cursor) item.
 *   <li><b>Extract</b> ({@code typeIndex >= 0}): extracts {@code amount} items at that grid index,
 *       placing them in the cursor ({@code toCursor = true}) or directly into inventory.
 * </ul>
 */
public record TremendousBackpackInteractPacket(
        int slotType, int slotIndex, String slotId, int typeIndex, int amount, boolean toCursor)
        implements CustomPacketPayload {

    public static final Type<TremendousBackpackInteractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "tremendous_backpack_interact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TremendousBackpackInteractPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    TremendousBackpackInteractPacket::slotType,
                    ByteBufCodecs.INT,
                    TremendousBackpackInteractPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    TremendousBackpackInteractPacket::slotId,
                    ByteBufCodecs.INT,
                    TremendousBackpackInteractPacket::typeIndex,
                    ByteBufCodecs.INT,
                    TremendousBackpackInteractPacket::amount,
                    ByteBufCodecs.BOOL,
                    TremendousBackpackInteractPacket::toCursor,
                    TremendousBackpackInteractPacket::new);

    @Override
    public Type<TremendousBackpackInteractPacket> type() {
        return TYPE;
    }

    public static void handle(TremendousBackpackInteractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack backpackStack = TremendousBackpackItem.getBackpackStack(
                    player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;
            TremendousBackpackContents contents = backpackStack.getOrDefault(
                    Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), TremendousBackpackContents.EMPTY);

            if (packet.typeIndex() == -1) {
                // Insert player's cursor item into the backpack
                ItemStack carried = player.containerMenu.getCarried();
                if (carried.isEmpty()) return;
                Object[] result = contents.withInserted(carried, carried.getCount());
                long remainder = (long) result[0];
                TremendousBackpackContents updated = (TremendousBackpackContents) result[1];
                backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
                TremendousBackpackItem.setBackpackStack(
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
                TremendousBackpackContents updated = (TremendousBackpackContents) result[1];
                if (extracted.isEmpty()) return;
                backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), updated);
                TremendousBackpackItem.setBackpackStack(
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
