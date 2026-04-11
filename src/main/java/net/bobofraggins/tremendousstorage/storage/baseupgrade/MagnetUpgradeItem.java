package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that grants a magnet ability to Tremendous Chests, Tremendous Backpacks
 * (placed), and Filing Cabinets.
 *
 * <p>Right-clicking on a supported block applies the upgrade, consuming the item.
 * Once upgraded, the block scans a 3-block radius every server tick for {@link net.minecraft.world.entity.item.ItemEntity}
 * instances whose item type is already stored in the inventory, absorbing them automatically.
 */
public class MagnetUpgradeItem extends Item {

    public MagnetUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        boolean matches = false;
        if (be instanceof ChestBlockEntity chest && !chest.hasMagnetUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                chest.setMagnetUpgrade(true);
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
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
        }

        return InteractionResult.PASS;
    }
}
