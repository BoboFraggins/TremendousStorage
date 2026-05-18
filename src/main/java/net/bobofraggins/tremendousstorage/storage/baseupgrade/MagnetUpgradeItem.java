package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that grants a magnet ability to Tremendous Chests, Tremendous Backpacks
 * (placed), Tremendous Tanks, and Filing Cabinets (and their Ender variants).
 *
 * <p>Right-clicking on a supported block applies the upgrade, consuming the item.
 * For item-storage blocks, the magnet scans a 3-block radius each tick for matching item entities.
 * For tanks, it scans for item entities containing matching fluid and drains them.
 */
public class MagnetUpgradeItem extends Item {

    public MagnetUpgradeItem(Item.Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltip,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.tremendousstorage.magnet_upgrade.tooltip"));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        boolean matches = false;
        if (be instanceof ChestBlockEntity chest && chest.isUpgradeable() && !chest.hasMagnetUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                chest.setMagnetUpgrade(true);
            }
            matches = true;
        } else if (be instanceof TankBlockEntity tank && !tank.hasMagnetUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                tank.setMagnetUpgrade(true);
            }
            matches = true;
        } else if (be instanceof FilingCabinetBlockEntity cabinet && !cabinet.hasMagnetUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                cabinet.setMagnetUpgrade(true);
            }
            matches = true;
        }

        if (matches) {
            if (!ctx.getLevel().isClientSide()) {
                ctx.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
