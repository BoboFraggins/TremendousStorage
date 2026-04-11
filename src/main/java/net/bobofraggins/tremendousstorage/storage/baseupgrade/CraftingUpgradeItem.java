package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that grants a crafting grid to a Tremendous Chest, Tremendous Backpack
 * (placed), or Storage Access Terminal.
 *
 * <p>Right-clicking on a supported placed block applies the upgrade in-place, consuming
 * the item. The flag is stored in block-entity NBT and survives break/replace.
 */
public class CraftingUpgradeItem extends Item {

    public CraftingUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());

        boolean matches = false;
        if (be instanceof ChestBlockEntity chest && !chest.hasCraftingUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                chest.setCraftingUpgrade(true);
            }
            matches = true;
        } else if (be instanceof AccessTerminalBlockEntity terminal && !terminal.hasCraftingUpgrade()) {
            if (!ctx.getLevel().isClientSide()) {
                terminal.setCraftingUpgrade(true);
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
