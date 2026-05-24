package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.crafting.AbstractCraftingUpgradeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: clear the crafting grid of any open
 * {@link AbstractCraftingUpgradeMenu}.
 *
 * <p>When {@code toStorage} is {@code true} each item is returned to the backing
 * storage (chest, backpack, or network); any overflow goes to the player's
 * inventory. When {@code false} all items go directly to the player's inventory.
 */
public record ClearCraftingGridPacket(boolean toStorage) implements CustomPacketPayload {

    public static final Type<ClearCraftingGridPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "clear_crafting_grid"));

    public static final StreamCodec<FriendlyByteBuf, ClearCraftingGridPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeBoolean(pkt.toStorage()), buf -> new ClearCraftingGridPacket(buf.readBoolean()));

    @Override
    public Type<ClearCraftingGridPacket> type() {
        return TYPE;
    }

    public static void handle(ClearCraftingGridPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof AbstractCraftingUpgradeMenu menu)) return;
            if (!menu.hasCraftingUpgrade()) return;
            menu.clearCraftingGrid(player, packet.toStorage());
        });
    }
}
