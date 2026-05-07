package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that grants a puller ability to Tremendous Chests, Tremendous Backpacks,
 * Picnic Baskets, Tremendous Tanks, and Filing Cabinets (and their Ender variants).
 *
 * <p>Right-clicking on a supported block applies the upgrade, consuming the item.
 * Once upgraded, the block's config UI shows six side-toggle buttons to configure
 * which faces the puller should pull from.
 */
public class PullerUpgradeItem extends Item {

    public PullerUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        boolean matches = false;
        if (be instanceof net.bobofraggins.tremendousstorage.storage.armorycabinet.ArmoryCabinetBlockEntity cabinet
                && !cabinet.hasPullerUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                cabinet.setPullerUpgrade(true);
            }
            matches = true;
        } else if (be instanceof BarrelBlockEntity barrel && !barrel.hasPullerUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                barrel.setPullerUpgrade(true);
            }
            matches = true;
        } else if (be instanceof ChestBlockEntity chest && !chest.hasPullerUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                chest.setPullerUpgrade(true);
            }
            matches = true;
        } else if (be instanceof TankBlockEntity tank && !tank.hasPullerUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                tank.setPullerUpgrade(true);
            }
            matches = true;
        } else if (be instanceof FilingCabinetBlockEntity cabinet && !cabinet.hasPullerUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                cabinet.setPullerUpgrade(true);
            }
            matches = true;
        }

        if (matches) {
            if (!ctx.getLevel().isClientSide()) {
                ctx.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
        }

        return InteractionResult.PASS;
    }
}
