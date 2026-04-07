package net.bobofraggins.tremendousstorage.storage.tremendoustank;

import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.storage.TieredBlockItem;
import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Block item for the Tremendous Tank.
 *
 * <p>Extends {@link TieredBlockItem} to append the storage tier to the display name.
 *
 * <p>Overrides {@link #useOn} to let players fill or drain the tank item directly against
 * fluid source blocks or other {@code IFluidHandler.BLOCK} targets. Tank-to-tank interactions
 * (clicking a {@link TremendousTankBlock} with this item) are already handled by
 * {@link TremendousTankBlock#useItemOn}, which intercepts first via the block's capability.
 *
 * <p>Shows a tooltip with the stored fluid name and fill level when the tank holds fluid.
 */
public class TremendousTankItem extends TieredBlockItem {

    public TremendousTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        TremendousTankContents contents =
                stack.getOrDefault(Registration.TREMENDOUS_TANK_CONTENTS.get(), TremendousTankContents.EMPTY);
        if (contents.isLocked()) {
            long cap = tierCapacity(stack);
            lines.add(Component.translatable(
                            "item.tremendousstorage.tremendous_tank.tooltip",
                            CountFormat.format(contents.amount()),
                            CountFormat.format(cap),
                            contents.storedFluid().getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    static long tierCapacity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return TremendousTankBlockEntity.BASE_CAPACITY;
        StorageTier tier = StorageTier.fromId(data.copyTag().getString("Tier"));
        return tier.getScaledCapacity(TremendousTankBlockEntity.BASE_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // Fluid interaction
    // -------------------------------------------------------------------------

    /**
     * Right-clicking a fluid source block or any {@code IFluidHandler.BLOCK} fills or drains
     * this tank item via its registered {@link TremendousTankItemFluidHandler} capability.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        boolean success = FluidUtil.interactWithFluidHandler(
                player, context.getHand(), level, context.getClickedPos(), context.getClickedFace());
        return success ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
