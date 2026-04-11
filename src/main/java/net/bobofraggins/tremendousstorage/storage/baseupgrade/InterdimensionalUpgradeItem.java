package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Upgrade item that grants interdimensional Personal Access Terminal connectivity to a Wireless Hub.
 *
 * <p>Can only be applied to a Netherite-tier hub (infinite-range tier). Right-clicking on a
 * supported block applies the upgrade, consuming the item. Via smithing table, the hub item
 * must likewise already be Netherite tier.
 *
 * <p>Once upgraded, the PAT can open the network UI from any dimension, not just the one the
 * Network Interface resides in.
 */
public class InterdimensionalUpgradeItem extends Item {

    public InterdimensionalUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.PASS;

        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof WirelessHubBlockEntity hub)) return InteractionResult.PASS;

        if (hub.getTier() != StorageTier.NETHERITE) {
            ctx.getPlayer().displayClientMessage(
                    Component.translatable("item.tremendousstorage.interdimensional_upgrade.requires_netherite"),
                    true);
            return InteractionResult.FAIL;
        }

        if (hub.hasInterdimensionalUpgrade()) return InteractionResult.FAIL;

        hub.setInterdimensionalUpgrade(true);
        ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
