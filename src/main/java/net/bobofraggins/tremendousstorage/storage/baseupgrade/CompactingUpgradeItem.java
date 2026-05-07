package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CompactingUpgradeItem extends Item {

    public CompactingUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof BarrelBlockEntity barrel) || barrel.hasCompactingUpgrade()) {
            return InteractionResult.PASS;
        }
        if (!ctx.getLevel().isClientSide()) {
            barrel.setCompactingUpgrade(true);
            ItemStack held = ctx.getItemInHand();
            held.shrink(1);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
    }
}
