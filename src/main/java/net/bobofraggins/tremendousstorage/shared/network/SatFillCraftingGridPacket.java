package net.bobofraggins.tremendousstorage.shared.network;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet: fill the crafting grid of an Access Terminal from the network.
 *
 * <p>Sent by the EMI / REI recipe transfer handlers. The server looks up the recipe, clears
 * the current grid contents back into the network, then extracts one ingredient per slot from
 * the network and places them in the crafting grid.
 */
public record SatFillCraftingGridPacket(BlockPos satPos, ResourceLocation recipeId) implements CustomPacketPayload {

    public static final Type<SatFillCraftingGridPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "sat_fill_crafting_grid"));

    public static final StreamCodec<FriendlyByteBuf, SatFillCraftingGridPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SatFillCraftingGridPacket::satPos,
            ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString),
            SatFillCraftingGridPacket::recipeId,
            SatFillCraftingGridPacket::new);

    @Override
    public Type<SatFillCraftingGridPacket> type() {
        return TYPE;
    }

    public static void handle(SatFillCraftingGridPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof AccessTerminalMenu menu)) return;
            if (!menu.getSatPos().equals(packet.satPos())) return;
            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            serverLevel
                    .getRecipeManager()
                    .byKey(packet.recipeId())
                    .filter(h -> h.value() instanceof CraftingRecipe)
                    .ifPresent(h -> menu.fillCraftingGridFromNetwork(serverLevel, (CraftingRecipe) h.value()));
        });
    }
}
