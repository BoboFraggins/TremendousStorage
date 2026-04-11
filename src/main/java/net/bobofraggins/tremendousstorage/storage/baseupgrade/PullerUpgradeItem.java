package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.filingcabinet.FilingCabinetBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that grants a puller ability to Tremendous Chests and Filing Cabinets.
 *
 * <p>Right-clicking on a supported block applies the upgrade, consuming the item.
 * Once upgraded, the block's config UI shows six side-toggle buttons to configure
 * which faces the puller should pull items from.
 */
public class PullerUpgradeItem extends Item {

    public PullerUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var block = ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock();
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        boolean matches = false;
        if (block == Registration.TREMENDOUS_CHEST.get()
                && be instanceof ChestBlockEntity chest
                && !chest.hasPullerUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                chest.setPullerUpgrade(true);
            }
            matches = true;
        } else if (block == Registration.FILING_CABINET.get()
                && be instanceof FilingCabinetBlockEntity cabinet
                && !cabinet.hasPullerUpgrade()) {
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
