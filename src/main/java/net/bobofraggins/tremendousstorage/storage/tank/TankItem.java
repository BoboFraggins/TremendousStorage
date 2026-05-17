package net.bobofraggins.tremendousstorage.storage.tank;

import java.util.Optional;
import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * Block item for the Tank.
 *
 * <p>Extends {@link TieredBlockItem} to append the storage tier to the display name.
 *
 * <p>In <b>Block Mode</b> (default): right-clicking a fluid source or {@code IFluidHandler.BLOCK}
 * fills/drains the item via its registered capability; right-clicking any other block places it.
 * Right-clicking on air toggles to Bucket Mode.
 *
 * <p>In <b>Bucket Mode</b>: all interaction is handled in {@link #use} via a
 * {@link ClipContext.Fluid#SOURCE_ONLY} raycast so the crosshair correctly resolves to fluid
 * source blocks. Right-clicking a matching source picks up one bucket; right-clicking anything
 * else places one bucket from the tank. Right-clicking on air (raycast miss) toggles back to
 * Block Mode.
 *
 * <p>The mode is stored in {@link TankContents#bucketMode()} and persists on the item.
 */
public class TankItem extends TieredBlockItem {

    public TankItem(Block block, Properties properties) {
        super(block, properties);
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, tooltipAdder, flag);
        TankContents contents = stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);
        if (contents.isLocked()) {
            long cap = tierCapacity(stack);
            tooltipAdder.accept(Component.translatable(
                            "item.tremendousstorage.tank.tooltip",
                            CountFormat.format(contents.amount()),
                            CountFormat.format(cap),
                            contents.storedFluid().getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (contents.bucketMode()) {
            tooltipAdder.accept(Component.translatable("item.tremendousstorage.tank.mode_bucket")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    static long tierCapacity(ItemStack stack) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return TankBlockEntity.BASE_CAPACITY;
        StorageTier tier = StorageTier.fromId(data.copyTagWithoutId().getStringOr("Tier", ""));
        return tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // use() — mode toggle and all Bucket Mode interactions
    // -------------------------------------------------------------------------

    /**
     * Handles mode toggle (right-click on air) and all Bucket Mode fluid interactions.
     *
     * <p>Bucket Mode uses {@link ClipContext.Fluid#SOURCE_ONLY} so the crosshair stops at fluid
     * source blocks rather than passing through them. Block Mode uses {@link ClipContext.Fluid#NONE}
     * only to check for a raycast miss (the toggle condition); actual block interaction is handled
     * by {@link #useOn}.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TankContents c = stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);

        // Raycast: SOURCE_ONLY in Bucket Mode (stops at fluid sources), NONE in Block Mode.
        BlockHitResult hit = getPlayerPOVHitResult(
                level, player, c.bucketMode() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);

        if (hit.getType() == HitResult.Type.MISS) {
            // Aiming at air/sky — toggle mode.
            if (!level.isClientSide()) {
                boolean newBucketMode = !c.bucketMode();
                stack.set(
                        Registration.TANK_CONTENTS.get(), new TankContents(c.storedFluid(), c.amount(), newBucketMode));
                player.sendOverlayMessage(Component.translatable(
                        newBucketMode
                                ? "item.tremendousstorage.tank.mode_bucket"
                                : "item.tremendousstorage.tank.mode_block"));
            }
            return InteractionResult.SUCCESS;
        }

        if (!c.bucketMode()) {
            // Block Mode: block-targeted clicks are handled by useOn.
            return InteractionResult.PASS;
        }

        // Bucket Mode: interact with the block the raycast hit.
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockState state = level.getBlockState(pos);

        // Try to pick up a matching fluid source block.
        if (level.getFluidState(pos).isSource() && state.getBlock() instanceof BucketPickup bucketPickup) {
            Fluid worldFluid = level.getFluidState(pos).getType();
            FluidStack worldFluidStack = new FluidStack(worldFluid, FluidType.BUCKET_VOLUME);
            boolean canAccept =
                    c.storedFluid().isEmpty() || FluidStack.isSameFluidSameComponents(c.storedFluid(), worldFluidStack);

            if (canAccept && tierCapacity(stack) - c.amount() >= FluidType.BUCKET_VOLUME) {
                if (!level.isClientSide()) {
                    Optional<SoundEvent> sound = bucketPickup.getPickupSound(state);
                    ItemStack picked = bucketPickup.pickupBlock(player, level, pos, state);
                    if (!picked.isEmpty()) {
                        FluidStack newType =
                                c.storedFluid().isEmpty() ? worldFluidStack.copyWithAmount(1) : c.storedFluid();
                        stack.set(
                                Registration.TANK_CONTENTS.get(),
                                new TankContents(newType, c.amount() + FluidType.BUCKET_VOLUME, true));
                        sound.ifPresent(s -> player.playSound(s, 1.0F, 1.0F));
                        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Wrong fluid or tank full — fall through to placement.
        }

        // Try to place one bucket of fluid from the tank into the world.
        if (!c.storedFluid().isEmpty() && c.amount() >= FluidType.BUCKET_VOLUME) {
            Fluid fluid = c.storedFluid().getFluid();
            boolean isLiquidContainer = state.getBlock() instanceof LiquidBlockContainer lbc
                    && lbc.canPlaceLiquid(player, level, pos, state, fluid);
            BlockPos targetPos = isLiquidContainer ? pos : pos.relative(face);

            if (!level.isClientSide()) {
                TankItemFluidHandler tankHandler = new TankItemFluidHandler(stack.copy());
                net.neoforged.neoforge.fluids.FluidStack placed =
                        FluidUtil.tryPlaceFluid(tankHandler, player, level, hand, targetPos);
                if (!placed.isEmpty()) {
                    player.setItemInHand(hand, tankHandler.getContainer());
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    // -------------------------------------------------------------------------
    // useOn() — Block Mode only; Bucket Mode defers to use()
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        TankContents c = stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);

        // In Bucket Mode all interaction is handled in use(); returning PASS here causes
        // the engine to call use() after this method.
        if (c.bucketMode()) {
            return InteractionResult.PASS;
        }

        // Block Mode: fill/drain via fluid handler, then fall through to block placement.
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            Player player = context.getPlayer();
            if (player != null) {
                boolean success = FluidUtil.interactWithFluidHandler(
                        player, context.getHand(), level, context.getClickedPos(), context.getClickedFace());
                if (success) return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}
