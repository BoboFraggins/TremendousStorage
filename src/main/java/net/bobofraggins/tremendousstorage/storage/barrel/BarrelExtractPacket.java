package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: extract from the barrel at {@code pos}.
 * Sent when the player left-clicks the item-display area on the barrel's front face.
 * {@code sneaking} true extracts a full vanilla stack; false extracts one item.
 */
public record BarrelExtractPacket(BlockPos pos, boolean sneaking) implements CustomPacketPayload {

    public static final Type<BarrelExtractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "barrel_extract"));

    public static final StreamCodec<FriendlyByteBuf, BarrelExtractPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            BarrelExtractPacket::pos,
            ByteBufCodecs.BOOL,
            BarrelExtractPacket::sneaking,
            BarrelExtractPacket::new);

    @Override
    public Type<BarrelExtractPacket> type() {
        return TYPE;
    }

    public static void handle(BarrelExtractPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            Level level = player.serverLevel();
            BlockState state = level.getBlockState(packet.pos());
            if (!(state.getBlock() instanceof BarrelBlock)) return;
            if (!(level.getBlockEntity(packet.pos()) instanceof BarrelBlockEntity be)) return;
            if (!be.isLocked() || be.getCount() == 0) return;

            if (be.hasCompactingUpgrade()) {
                Direction facing = state.getValue(BarrelBlock.FACING);
                BlockHitResult hit = (BlockHitResult) level.clip(new ClipContext(
                        player.getEyePosition(),
                        player.getEyePosition().add(player.getLookAngle().scale(5.0)),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player));
                if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(packet.pos())) return;
                int slot = BarrelBlock.getCompactingSlot(hit, facing);
                var handler = new CompactingBarrelItemHandler(be);
                int amount = packet.sneaking() ? handler.getStackInSlot(slot).getMaxStackSize() : 1;
                BarrelBlock.extractCompactingSlot(be, player, slot, amount);
            } else {
                int amount = packet.sneaking() ? be.getStoredItem().getMaxStackSize() : 1;
                ItemStack extracted = be.extract(amount, false);
                if (!extracted.isEmpty() && !player.addItem(extracted)) {
                    player.drop(extracted, false);
                }
            }
        });
    }
}
