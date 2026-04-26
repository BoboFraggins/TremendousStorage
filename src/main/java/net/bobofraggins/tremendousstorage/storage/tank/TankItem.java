package net.bobofraggins.tremendousstorage.storage.tank;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Block item for the Tank.
 *
 * <p>Extends {@link TieredBlockItem} to append the storage tier to the display name.
 *
 * <p>In <b>Block Mode</b> (default): right-clicking a fluid source or {@code IFluidHandler.BLOCK}
 * fills/drains the item via its registered capability; right-clicking any other block places it.
 *
 * <p>In <b>Bucket Mode</b>: right-clicking on air toggles back to Block Mode. Right-clicking a
 * matching fluid source picks up one bucket (1000 mB) into the tank; right-clicking anything
 * else attempts to place one bucket of tank fluid into the world.
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        TankContents contents = stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);
        if (contents.isLocked()) {
            long cap = tierCapacity(stack);
            lines.add(Component.translatable(
                            "item.tremendousstorage.tank.tooltip",
                            CountFormat.format(contents.amount()),
                            CountFormat.format(cap),
                            contents.storedFluid().getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (contents.bucketMode()) {
            lines.add(Component.translatable("item.tremendousstorage.tank.mode_bucket")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    static long tierCapacity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return TankBlockEntity.BASE_CAPACITY;
        StorageTier tier = StorageTier.fromId(data.copyTag().getString("Tier"));
        return tier.getScaledCapacity(TankBlockEntity.BASE_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // Mode toggle (right-click on air)
    // -------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Only toggle when not targeting a block; let useOn handle block-targeted clicks.
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            TankContents c = stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);
            boolean newBucketMode = !c.bucketMode();
            stack.set(Registration.TANK_CONTENTS.get(), new TankContents(c.storedFluid(), c.amount(), newBucketMode));
            player.displayClientMessage(
                    Component.translatable(
                            newBucketMode
                                    ? "item.tremendousstorage.tank.mode_bucket"
                                    : "item.tremendousstorage.tank.mode_block"),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // -------------------------------------------------------------------------
    // Block / Bucket Mode right-click on block
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        TankContents c = stack.getOrDefault(Registration.TANK_CONTENTS.get(), TankContents.EMPTY);

        if (c.bucketMode()) {
            if (!level.isClientSide() && player != null) {
                BlockPos pos = context.getClickedPos();
                Direction face = context.getClickedFace();
                InteractionHand hand = context.getHand();

                // Try to pick up a bucket of matching fluid from the world.
                FluidActionResult pickup = FluidUtil.tryPickUpFluid(stack, player, level, pos, face);
                if (pickup.isSuccess()) {
                    player.setItemInHand(hand, pickup.getResult());
                    return InteractionResult.SUCCESS;
                }

                // Try to place a bucket of fluid from the tank into the world.
                if (!c.storedFluid().isEmpty() && c.amount() >= FluidType.BUCKET_VOLUME) {
                    Fluid fluid = c.storedFluid().getFluid();
                    BlockState state = level.getBlockState(pos);
                    boolean isLiquidContainer = state.getBlock() instanceof LiquidBlockContainer lbc
                            && lbc.canPlaceLiquid(player, level, pos, state, fluid);
                    BlockPos targetPos = isLiquidContainer ? pos : pos.relative(face);
                    FluidStack toPlace = c.storedFluid().copyWithAmount(FluidType.BUCKET_VOLUME);
                    FluidActionResult place = FluidUtil.tryPlaceFluid(player, level, hand, targetPos, stack, toPlace);
                    if (place.isSuccess()) {
                        player.setItemInHand(hand, place.getResult());
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            // Client-side optimistic success prevents block-placement prediction.
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        // Block Mode: fill/drain via fluid handler, then fall through to block placement.
        if (!level.isClientSide()) {
            if (player != null) {
                boolean success = FluidUtil.interactWithFluidHandler(
                        player, context.getHand(), level, context.getClickedPos(), context.getClickedFace());
                if (success) return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}
