package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageKey;
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

/**
 * Client-to-server packet: quick-stack matching items from the player's inventory into a backpack.
 *
 * <p>Transfers items whose type already exists in the backpack, without introducing new item types.
 * Damageable items and items with non-default component data are excluded.
 */
public record BackpackQuickStackPacket(int slotType, int slotIndex, String slotId) implements CustomPacketPayload {

    public static final Type<BackpackQuickStackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "backpack_quick_stack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackQuickStackPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    BackpackQuickStackPacket::slotType,
                    ByteBufCodecs.INT,
                    BackpackQuickStackPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8,
                    BackpackQuickStackPacket::slotId,
                    BackpackQuickStackPacket::new);

    @Override
    public Type<BackpackQuickStackPacket> type() {
        return TYPE;
    }

    public static void handle(BackpackQuickStackPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack backpackStack =
                    BackpackItem.getBackpackStack(player, packet.slotType(), packet.slotIndex(), packet.slotId());
            if (backpackStack.isEmpty()) return;
            BackpackContents contents =
                    backpackStack.getOrDefault(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);

            boolean anyInserted = false;
            int size = player.getInventory().getContainerSize();
            for (int invSlot = 0; invSlot < size; invSlot++) {
                ItemStack stack = player.getInventory().getItem(invSlot);
                if (stack.isEmpty() || isExcluded(stack)) continue;
                if (!backpackContains(contents, stack)) continue;

                Object[] result = contents.withInserted(stack, stack.getCount());
                long remainder = (long) result[0];
                contents = (BackpackContents) result[1];
                int inserted = stack.getCount() - (int) remainder;
                if (inserted > 0) {
                    stack.shrink(inserted);
                    if (stack.isEmpty()) player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                    anyInserted = true;
                }
            }

            if (anyInserted) {
                backpackStack.set(Registration.TREMENDOUS_BACKPACK_CONTENTS.get(), contents);
                BackpackItem.setBackpackStack(
                        player, backpackStack, packet.slotType(), packet.slotIndex(), packet.slotId());
                player.getInventory().setChanged();
            }
        });
    }

    private static boolean isExcluded(ItemStack stack) {
        return stack.isDamageableItem() || !stack.isComponentsPatchEmpty();
    }

    private static boolean backpackContains(BackpackContents contents, ItemStack stack) {
        StorageKey key = StorageKey.of(stack);
        for (int i = 0; i < contents.typeCount(); i++) {
            if (StorageKey.of(contents.getType(i)).equals(key)) return true;
        }
        return false;
    }
}
