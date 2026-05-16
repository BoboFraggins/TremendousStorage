package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: cycle the selected recipe on an Access Terminal crafting grid.
 *
 * <p>When multiple recipes match the current ingredient layout, the player can cycle through
 * them using the up/down arrows rendered next to the crafting arrow. Direction +1 advances
 * to the next recipe, -1 goes to the previous.
 */
public record CycleRecipePacket(BlockPos satPos, int direction) implements CustomPacketPayload {

    public static final Type<CycleRecipePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "cycle_recipe"));

    public static final StreamCodec<FriendlyByteBuf, CycleRecipePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CycleRecipePacket::satPos,
            ByteBufCodecs.INT,
            CycleRecipePacket::direction,
            CycleRecipePacket::new);

    @Override
    public Type<CycleRecipePacket> type() {
        return TYPE;
    }

    public static void handle(CycleRecipePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof AccessTerminalMenu menu)) return;
            if (!menu.getSatPos().equals(packet.satPos())) return;
            menu.handleCycleRecipe(packet.direction());
        });
    }
}
