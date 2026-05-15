package net.bobofraggins.tremendousstorage.storage.baseupgrade;

import java.util.function.Consumer;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Upgrade item that adds HAARP weather-control functionality to the Wireless Hub.
 *
 * <p>Right-clicking on a placed Wireless Hub applies the upgrade in-place, consuming
 * the item. The flag is stored in block-entity NBT and survives break/replace (via
 * the BLOCK_ENTITY_DATA component on the dropped item).
 */
public class HaarpUpgradeItem extends Item {

    public HaarpUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltip,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.tremendousstorage.haarp_upgrade.tooltip"));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof WirelessHubBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (be.hasHaarpUpgrade()) {
            return InteractionResult.PASS; // already applied
        }
        if (!ctx.getLevel().isClientSide()) {
            be.setHaarpUpgrade(true);
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
